import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, from, map, of, switchMap } from 'rxjs';
import type { Observable } from 'rxjs';
import { UserAdministrationControllerService } from '../../api/generated';
import type { ScopeResponse, UserAssignmentCandidateResponse, UserResponse } from '../../api/generated';
import { PiipApiError, resolveApiUrl } from '../../core/piip-http.repository';
import type { AdministrableExecutingUnit } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';
import { EditUserAssignmentDialogComponent } from './edit-user-assignment-dialog.component';
import { NewUserAssignmentDialogComponent } from './new-user-assignment-dialog.component';
import { SuspendUserAssignmentDialogComponent } from './suspend-user-assignment-dialog.component';

type UserScope = ScopeResponse;
type UserItem = UserResponse;
type AssignmentCandidate = UserAssignmentCandidateResponse;
type Role = NonNullable<UserScope['role']>;
interface UserAssignmentRow { user: UserItem; scope: UserScope | null; }
interface UserAssignmentGroup { user: UserItem; scopes: UserScope[]; }
export interface AssignmentUser { subject: string; fullName: string; email: string; withoutAssignments: boolean; }
interface InstitutionItem { id: number; code: string; name: string; }

@Component({
  selector: 'app-user-administration',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, MatSnackBarModule, PiipPaginationComponent],
  templateUrl: './user-administration.component.html',
  styleUrl: './user-administration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserAdministrationComponent {
  private readonly userAdministration = inject(UserAdministrationControllerService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly apiUrl = resolveApiUrl();
  readonly repository = inject(PIIP_REPOSITORY);

  readonly users = signal<UserItem[]>([]);
  readonly assignmentCandidates = signal<AssignmentCandidate[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly assigning = signal(false);
  readonly editingScope = signal<UserScope | null>(null);
  readonly savingScopeId = signal<number | null>(null);
  readonly changingScopeId = signal<number | null>(null);
  readonly pageIndex = signal(0);
  readonly searchTerm = signal('');
  readonly roleFilter = signal<Role | 'ALL'>('ALL');
  readonly executingUnitFilter = signal<number | 'ALL'>('ALL');
  readonly stateFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly expandedSubjects = signal<ReadonlySet<string>>(new Set());
  readonly operationPending = computed(() => this.assigning() || this.savingScopeId() !== null || this.changingScopeId() !== null);
  readonly assignmentForm = this.formBuilder.nonNullable.group({
    userSubject: ['', Validators.required],
    role: ['CONSULTA_EXTERNA' as Role, Validators.required],
    institutionId: [0, Validators.min(1)],
    executingUnitId: [0],
  });
  readonly editForm = this.assignmentFormGroup();
  readonly assignmentRows = computed<UserAssignmentRow[]>(() => this.users().flatMap<UserAssignmentRow>((user) =>
    user.scopes?.length ? user.scopes.map((scope) => ({ user, scope })) : [{ user, scope: null }],
  ));
  readonly assignmentGroups = computed<UserAssignmentGroup[]>(() => this.users().map((user) => ({
    user,
    scopes: user.scopes ?? [],
  })));
  readonly assignmentUsers = computed<AssignmentUser[]>(() => {
    const usersBySubject = new Map<string, AssignmentUser>();
    for (const user of this.users()) {
      if (user.subject) {
        usersBySubject.set(user.subject, {
          subject: user.subject,
          fullName: user.fullName ?? user.subject,
          email: user.email ?? '',
          withoutAssignments: false,
        });
      }
    }
    for (const candidate of this.assignmentCandidates()) {
      if (candidate.subject) {
        const existingUser = usersBySubject.get(candidate.subject);
        usersBySubject.set(candidate.subject, existingUser ? {
          ...existingUser,
          withoutAssignments: true,
        } : {
          subject: candidate.subject,
          fullName: candidate.fullName ?? candidate.subject,
          email: candidate.email ?? '',
          withoutAssignments: true,
        });
      }
    }
    return Array.from(usersBySubject.values());
  });
  readonly filteredAssignmentGroups = computed(() => {
    const searchTerm = this.searchTerm().trim().toLocaleLowerCase();
    const role = this.roleFilter();
    const executingUnitId = this.executingUnitFilter();
    const state = this.stateFilter();
    return this.assignmentGroups().flatMap((group) => {
      if (searchTerm && !`${group.user.fullName ?? ''} ${group.user.email ?? ''} ${group.user.subject ?? ''}`.toLocaleLowerCase().includes(searchTerm)) return [];
      const scopes = group.scopes.filter((scope) => {
        if (role !== 'ALL' && scope.role !== role) return false;
        if (executingUnitId !== 'ALL' && scope.executingUnitId !== executingUnitId) return false;
        if (state === 'ACTIVE' && !scope.active) return false;
        if (state === 'INACTIVE' && scope.active !== false) return false;
        return true;
      });
      const hasScopeFilter = role !== 'ALL' || executingUnitId !== 'ALL' || state !== 'ALL';
      return !hasScopeFilter || scopes.length ? [{ ...group, scopes }] : [];
    });
  });
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.filteredAssignmentGroups().length));
  readonly pagedAssignmentGroups = computed(() => paginateItems(this.filteredAssignmentGroups(), this.currentPage()));
  readonly visibleExecutingUnits = computed(() => {
    const units = new Map<number, string>();
    for (const { scope } of this.assignmentRows()) {
      if (scope?.executingUnitId !== undefined && scope.executingUnit) units.set(scope.executingUnitId, scope.executingUnit);
    }
    return Array.from(units, ([id, name]) => ({ id, name }));
  });
  readonly hasActiveFilters = computed(() => this.searchTerm() !== '' || this.roleFilter() !== 'ALL' || this.executingUnitFilter() !== 'ALL' || this.stateFilter() !== 'ALL');
  readonly administrableInstitutions = computed(() => {
    return this.repository.administrableScopes().map((scope) => ({
      id: scope.institutionId,
      code: scope.institutionCode,
      name: scope.institutionName,
    }));
  });
  readonly activeAdministratorUnit = computed(() => {
    const executingUnitId = this.repository.selectedExecutingUnitId();
    if (!this.repository.canAdministerExecutingUnit(executingUnitId)) return undefined;
    return this.repository.executingUnits().find((unit) => unit.id === executingUnitId);
  });

  constructor() {
    this.userAdministration.rootUrl = this.apiUrl;
    this.load();
  }

  openEdit(scope: UserScope): void {
    if (this.operationPending() || !scope.active || scope.id === undefined || scope.version === undefined || !scope.role || scope.institutionId === undefined) return;
    this.editingScope.set(scope);
    this.editForm.setValue({
      role: scope.role,
      institutionId: scope.institutionId,
      executingUnitId: scope.executingUnitId ?? 0,
    });
    const user = this.users().find((item) => item.scopes?.some((itemScope) => itemScope.id === scope.id));
    this.dialog.open(EditUserAssignmentDialogComponent, {
      data: {
        scope,
        userName: user?.fullName ?? 'Usuario autorizado',
        userEmail: user?.email ?? '',
        administrableScopes: this.repository.administrableScopes(),
      },
      autoFocus: 'first-header',
    }).afterClosed().subscribe((value) => {
      if (!value || this.editingScope()?.id !== scope.id) {
        this.editingScope.set(null);
        return;
      }
      this.editForm.setValue(value);
      this.saveEdit();
    });
  }

  openAssignment(): void {
    if (this.operationPending()) return;
    this.dialog.open(NewUserAssignmentDialogComponent, {
      data: {
        form: this.assignmentForm,
        users: this.assignmentUsers(),
        administrableScopes: this.repository.administrableScopes(),
      },
    }).afterClosed().subscribe((shouldAssign) => {
      if (shouldAssign) this.assign();
    });
  }

  setSearchTerm(value: string): void {
    this.searchTerm.set(value);
    this.pageIndex.set(0);
  }

  setRoleFilter(value: string): void {
    this.roleFilter.set(value === 'ALL' ? 'ALL' : value as Role);
    this.pageIndex.set(0);
  }

  setExecutingUnitFilter(value: string): void {
    this.executingUnitFilter.set(value === 'ALL' ? 'ALL' : Number(value));
    this.pageIndex.set(0);
  }

  setStateFilter(value: string): void {
    this.stateFilter.set(value as 'ALL' | 'ACTIVE' | 'INACTIVE');
    this.pageIndex.set(0);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.roleFilter.set('ALL');
    this.executingUnitFilter.set('ALL');
    this.stateFilter.set('ALL');
    this.pageIndex.set(0);
  }

  toggleGroup(subject: string | undefined): void {
    if (!subject) return;
    const expanded = new Set(this.expandedSubjects());
    expanded.has(subject) ? expanded.delete(subject) : expanded.add(subject);
    this.expandedSubjects.set(expanded);
  }

  isExpanded(subject: string | undefined): boolean {
    return subject !== undefined && this.expandedSubjects().has(subject);
  }

  userInitials(user: UserItem): string {
    const names = (user.fullName ?? user.subject ?? '?').trim().split(/\s+/).filter(Boolean);
    return names.slice(0, 2).map((name) => name[0]).join('').toUpperCase();
  }

  roleLabel(scope: UserScope): string {
    if (scope.role === 'ADMINISTRADOR_PIIP') return 'Administrador PIIP';
    if (scope.role === 'CONSULTA_EXTERNA') return 'Consulta externa';
    return 'Sin rol asignado';
  }

  scopeAreaLabel(scope: UserScope): string {
    return scope.executingUnitId === undefined ? 'Toda la institución' : scope.executingUnit ?? 'Ámbito no disponible';
  }

  activeAssignments(group: UserAssignmentGroup): number {
    return group.scopes.filter((scope) => scope.active).length;
  }

  suspendedAssignments(group: UserAssignmentGroup): number {
    return group.scopes.filter((scope) => !scope.active).length;
  }

  saveEdit(): void {
    const scope = this.editingScope();
    if (this.operationPending() || !scope || scope.id === undefined || scope.version === undefined) return;
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) return;

    const value = this.editForm.getRawValue();
    if (!this.isAdministrableScope(value.institutionId, value.executingUnitId)) {
      this.snackBar.open('El ámbito seleccionado no está cubierto por tus asignaciones de Administrador PIIP.', 'Cerrar', { duration: 4200 });
      return;
    }
    if (value.executingUnitId === 0 && !this.confirmInstitutionWide('editar', value.institutionId)) return;
    this.savingScopeId.set(scope.id);
    this.userAdministration.update({
      scopeId: scope.id,
      version: scope.version,
      body: { role: value.role, institutionId: value.institutionId, executingUnitId: value.executingUnitId || undefined },
    }).pipe(finalize(() => this.savingScopeId.set(null))).subscribe({
      next: () => {
        this.editingScope.set(null);
        this.snackBar.open('Asignación actualizada.', 'Cerrar', { duration: 2600 });
        this.load();
      },
      error: (response) => this.showOperationError(response, 'No se pudo actualizar la asignación.'),
    });
  }

  suspend(scope: UserScope): void {
    if (this.operationPending() || scope.id === undefined || scope.version === undefined || !scope.active) return;
    const user = this.userForScope(scope);
    this.dialog.open(SuspendUserAssignmentDialogComponent, {
      data: {
        scope,
        userName: user?.fullName ?? 'Usuario autorizado',
        userEmail: user?.email ?? '',
      },
      autoFocus: 'dialog',
      restoreFocus: true,
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.changeAssignmentState(scope, 'SUSPEND');
    });
  }

  reactivate(scope: UserScope): void {
    if (this.operationPending() || scope.id === undefined || scope.version === undefined || scope.active) return;
    this.changeAssignmentState(scope, 'REACTIVATE');
  }

  private changeAssignmentState(scope: UserScope, action: 'SUSPEND' | 'REACTIVATE'): void {
    if (this.operationPending() || scope.id === undefined || scope.version === undefined) return;
    const affectsCurrentUser = this.scopeBelongsToCurrentUser(scope);
    const activeExecutingUnitId = this.repository.selectedExecutingUnitId();
    this.changingScopeId.set(scope.id);
    const request: Observable<unknown> = action === 'SUSPEND'
      ? this.userAdministration.suspend({ scopeId: scope.id, version: scope.version })
      : this.userAdministration.reactivate({ scopeId: scope.id, version: scope.version });
    request.pipe(
      switchMap(() => affectsCurrentUser
        ? from(Promise.resolve(this.repository.refreshAuthorizationContext())).pipe(
            map(() => 'REFRESHED' as const),
            catchError(() => of('FAILED' as const)),
          )
        : of('NOT_REQUIRED' as const)),
      finalize(() => this.changingScopeId.set(null)),
    )
      .subscribe({
        next: (refreshState) => this.finishAssignmentStateChange(action, refreshState, activeExecutingUnitId),
        error: (response) => this.showOperationError(
          response,
          action === 'SUSPEND' ? 'No se pudo suspender la asignación.' : 'No se pudo reactivar la asignación.',
        ),
      });
  }

  private finishAssignmentStateChange(
    action: 'SUSPEND' | 'REACTIVATE',
    refreshState: 'REFRESHED' | 'FAILED' | 'NOT_REQUIRED',
    previousActiveExecutingUnitId: number | null,
  ): void {
    if (refreshState === 'FAILED') {
      this.snackBar.open(
        'La asignación cambió, pero no fue posible actualizar tu acceso. Recarga la página para sincronizarlo.',
        'Cerrar',
        { duration: 5200 },
      );
      this.load();
      return;
    }
    if (refreshState === 'REFRESHED'
        && !this.repository.canAdministerExecutingUnit(previousActiveExecutingUnitId)) {
      this.clearAdministrationView();
      this.snackBar.open(
        'Saliste de Administración de usuarios porque la UE activa ya no tiene rol Administrador PIIP.',
        'Cerrar',
        { duration: 5200 },
      );
      void this.router.navigateByUrl('/inicio');
      return;
    }
    this.snackBar.open(action === 'SUSPEND' ? 'Asignación suspendida.' : 'Asignación reactivada.', 'Cerrar', { duration: 2600 });
    this.load();
  }

  private clearAdministrationView(): void {
    this.users.set([]);
    this.assignmentCandidates.set([]);
    this.repository.administrableScopes.set([]);
    this.expandedSubjects.set(new Set());
    this.error.set('');
    this.loading.set(false);
  }

  private scopeBelongsToCurrentUser(scope: UserScope): boolean {
    const subject = this.repository.currentUser()?.subject;
    return subject !== undefined && this.userForScope(scope)?.subject === subject;
  }

  private userForScope(scope: UserScope): UserItem | undefined {
    return this.users().find((item) => item.scopes?.some((itemScope) => itemScope.id === scope.id));
  }

  assign(): void {
    if (this.operationPending()) return;
    this.assignmentForm.markAllAsTouched();
    if (this.assignmentForm.invalid) return;
    const value = this.assignmentForm.getRawValue();
    if (!this.isAdministrableScope(value.institutionId, value.executingUnitId)) {
      this.snackBar.open('El ámbito seleccionado no está cubierto por tus asignaciones de Administrador PIIP.', 'Cerrar', { duration: 4200 });
      return;
    }
    if (this.hasVisibleActiveDuplicate(value)) {
      this.snackBar.open('El usuario ya cuenta con una asignación activa igual. Elige otro rol o ámbito.', 'Cerrar', { duration: 4200 });
      return;
    }
    if (value.executingUnitId === 0 && !this.confirmInstitutionWide('crear', value.institutionId)) return;
    this.assigning.set(true);
    this.userAdministration.assign({ body: { ...value, executingUnitId: value.executingUnitId || undefined } })
      .pipe(finalize(() => this.assigning.set(false)))
      .subscribe({
        next: () => { this.snackBar.open('Rol asignado.', 'Cerrar', { duration: 2600 }); this.load(); },
        error: (response) => this.showOperationError(response, 'No se pudo crear la asignación.'),
      });
  }

  private assignmentFormGroup() {
    return this.formBuilder.nonNullable.group({
      role: ['CONSULTA_EXTERNA' as Role, Validators.required],
      institutionId: [0, Validators.min(1)],
      executingUnitId: [0],
    });
  }

  assignmentExecutingUnits(): AdministrableExecutingUnit[] {
    return this.administrableExecutingUnits(this.assignmentForm.controls.institutionId.value);
  }

  canUseInstitutionWide(institutionId: number): boolean {
    return this.repository.administrableScopes()
      .some((scope) => scope.institutionId === institutionId && scope.institutionWideAllowed);
  }

  private administrableExecutingUnits(institutionId: number): AdministrableExecutingUnit[] {
    return this.repository.administrableScopes()
      .find((scope) => scope.institutionId === institutionId)?.executingUnits ?? [];
  }

  private isAdministrableScope(institutionId: number, executingUnitId: number): boolean {
    if (executingUnitId === 0) return this.canUseInstitutionWide(institutionId);
    return this.administrableExecutingUnits(institutionId).some((unit) => unit.id === executingUnitId);
  }

  private confirmInstitutionWide(action: 'crear' | 'editar', institutionId: number): boolean {
    const institution = this.administrableInstitutions().find((item) => item.id === institutionId);
    const verb = action === 'crear' ? 'crear esta asignación' : 'guardar este cambio';
    return window.confirm(
      `Vas a ${verb} con alcance para toda la institución ${institution?.name ?? institutionId}. Esto cubrirá todas sus Unidades Ejecutoras. ¿Deseas continuar?`,
    );
  }

  private load(): void {
    this.loading.set(true); this.error.set('');
    forkJoin({
      users: this.userAdministration.users().pipe(
        switchMap((users) => this.readGeneratedList<UserItem>(users)),
        catchError(() => { this.error.set('No fue posible consultar la API de usuarios.'); return of([] as UserItem[]); }),
      ),
      candidates: this.userAdministration.assignmentCandidates().pipe(
        switchMap((candidates) => this.readGeneratedList<AssignmentCandidate>(candidates)),
        catchError(() => of([] as AssignmentCandidate[])),
      ),
      administrableScopes: from(Promise.resolve(this.repository.loadAdministrableScopes())).pipe(
        catchError((response) => {
          const status = response instanceof HttpErrorResponse || response instanceof PiipApiError ? response.status : undefined;
          this.error.set(status === 403
            ? 'Ya no tienes autorización para consultar la administración de usuarios.'
            : 'No fue posible consultar los ámbitos administrativos.');
          return of(undefined);
        }),
      ),
    }).pipe(finalize(() => this.loading.set(false))).subscribe(({ users, candidates }) => {
      this.users.set(users);
      this.assignmentCandidates.set(candidates);
    });
  }

  private hasVisibleActiveDuplicate(value: { userSubject: string; role: Role; institutionId: number; executingUnitId: number }): boolean {
    return this.users().some((user) => user.subject === value.userSubject && user.scopes?.some((scope) =>
      scope.active
      && scope.role === value.role
      && scope.institutionId === value.institutionId
      && this.normalizedExecutingUnitId(scope.executingUnitId) === this.normalizedExecutingUnitId(value.executingUnitId),
    ));
  }

  private normalizedExecutingUnitId(executingUnitId: number | undefined): number | null {
    return executingUnitId || null;
  }

  private readGeneratedList<T>(response: T[] | Blob): Observable<T[]> {
    if (!(response instanceof Blob)) return of(response);
    return from(response.text()).pipe(map((content) => JSON.parse(content) as T[]));
  }

  private showOperationError(error: unknown, fallback: string): void {
    const status = error instanceof HttpErrorResponse ? error.status : undefined;
    const message = status === 403
      ? 'No tienes autorización para realizar esta operación.'
      : status === 409
        ? 'La información cambió. Actualiza la pantalla antes de volver a intentarlo.'
        : status === 422
          ? 'La operación no cumple las validaciones de rol, ámbito o cobertura.'
          : fallback;
    this.snackBar.open(message, 'Cerrar', { duration: 4200 });
  }
}
