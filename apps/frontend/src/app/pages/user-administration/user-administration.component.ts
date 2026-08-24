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
import { catchError, from, map, of, switchMap } from 'rxjs';
import { PiipApiError } from '../../core/piip-http.repository';
import { AuthorizationRecoveryService } from '../../core/authorization-recovery.service';
import type { AdministrableExecutingUnit, AssignmentRole, UserAdministrationUser, UserAssignmentCandidate, UserAssignmentScope } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';
import { EditUserAssignmentDialogComponent } from './edit-user-assignment-dialog.component';
import { NewUserAssignmentDialogComponent } from './new-user-assignment-dialog.component';
import { SuspendUserAssignmentDialogComponent } from './suspend-user-assignment-dialog.component';

type UserScope = UserAssignmentScope;
type UserItem = UserAdministrationUser;
type AssignmentCandidate = UserAssignmentCandidate;
type Role = AssignmentRole;
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
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly authorizationRecovery = inject(AuthorizationRecoveryService);

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
    const affectsCurrentUser = this.scopeBelongsToCurrentUser(scope);
    const previousActiveExecutingUnitId = this.repository.selectedExecutingUnitId();
    this.savingScopeId.set(scope.id);
    from(Promise.resolve(this.repository.updateUserAssignment(scope.id, scope.version, {
      role: value.role,
      institutionId: value.institutionId,
      executingUnitId: value.executingUnitId || undefined,
    }))).subscribe({
      next: () => void this.finishSuccessfulMutation(
        'Asignación actualizada.',
        affectsCurrentUser,
        previousActiveExecutingUnitId,
        () => this.editingScope.set(null),
        () => this.savingScopeId.set(null),
      ),
      error: (response) => { this.savingScopeId.set(null); this.showOperationError(response, 'No se pudo actualizar la asignación.'); },
    });
  }

  suspend(scope: UserScope): void {
    if (this.operationPending() || scope.id === undefined || scope.version === undefined || !scope.active || this.isSelfAdministrator(scope)) return;
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
    const request = action === 'SUSPEND'
      ? this.repository.suspendUserAssignment(scope.id, scope.version)
      : this.repository.reactivateUserAssignment(scope.id, scope.version);
    from(Promise.resolve(request)).pipe(
      switchMap(() => affectsCurrentUser
        ? from(Promise.resolve(this.repository.refreshAuthorizationContext())).pipe(
            map(() => 'REFRESHED' as const),
            catchError(() => of('FAILED' as const)),
          )
        : of('NOT_REQUIRED' as const)),
    )
      .subscribe({
        next: (refreshState) => void this.finishAssignmentStateChange(action, refreshState, activeExecutingUnitId),
        error: (response) => {
          this.changingScopeId.set(null);
          this.showOperationError(response, action === 'SUSPEND' ? 'No se pudo suspender la asignación.' : 'No se pudo reactivar la asignación.');
        },
      });
  }

  private async finishAssignmentStateChange(
    action: 'SUSPEND' | 'REACTIVATE',
    refreshState: 'REFRESHED' | 'FAILED' | 'NOT_REQUIRED',
    previousActiveExecutingUnitId: number | null,
  ): Promise<void> {
    try {
      if (refreshState === 'FAILED') {
        throw new Error('No fue posible actualizar tu acceso.');
      }
      if (refreshState === 'REFRESHED' && !this.repository.canAdministerExecutingUnit(previousActiveExecutingUnitId)) {
        await this.failClosed('Saliste de Administración de usuarios porque la UE activa ya no tiene rol Administrador PIIP.');
        return;
      }
      await this.load();
      this.snackBar.open(action === 'SUSPEND' ? 'Asignación suspendida.' : 'Asignación reactivada.', 'Cerrar', { duration: 2600 });
    } catch {
      await this.failClosed('La asignación cambió, pero no fue posible actualizar tu acceso. Se cerró esta vista por seguridad.');
    } finally {
      this.changingScopeId.set(null);
    }
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

  isSelfAdministrator(scope: UserScope): boolean {
    return scope.role === 'ADMINISTRADOR_PIIP' && this.scopeBelongsToCurrentUser(scope);
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
    const affectsCurrentUser = value.userSubject === this.repository.currentUser()?.subject;
    const previousActiveExecutingUnitId = this.repository.selectedExecutingUnitId();
    this.assigning.set(true);
    from(Promise.resolve(this.repository.assignUserRole({
      userSubject: value.userSubject,
      role: value.role,
      institutionId: value.institutionId,
      executingUnitId: value.executingUnitId || undefined,
    })))
      .subscribe({
        next: (result) => void this.finishSuccessfulMutation(
          result.outcome === 'REACTIVATED' ? 'Asignación reactivada.' : 'Rol asignado.',
          affectsCurrentUser,
          previousActiveExecutingUnitId,
          undefined,
          () => this.assigning.set(false),
        ),
        error: (response) => { this.assigning.set(false); this.showOperationError(response, 'No se pudo crear la asignación.'); },
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

  private load(): Promise<void> {
    this.loading.set(true); this.error.set('');
    return Promise.all([this.repository.loadUserAdministration(), this.repository.loadAdministrableScopes()])
      .then(([snapshot]) => {
        this.users.set(snapshot.users);
        this.assignmentCandidates.set(snapshot.assignmentCandidates);
      })
      .catch((response: unknown) => {
        const status = response instanceof PiipApiError ? response.status : undefined;
        this.error.set(status === 403
          ? 'Ya no tienes autorización para consultar la administración de usuarios.'
          : 'No fue posible consultar la administración de usuarios.');
      })
      .finally(() => this.loading.set(false));
  }

  private async finishSuccessfulMutation(
    message: string,
    affectsCurrentUser: boolean,
    previousActiveExecutingUnitId: number | null,
    onReconciled: (() => void) | undefined,
    releaseOperation: () => void,
  ): Promise<void> {
    try {
      if (affectsCurrentUser) {
        await Promise.resolve(this.repository.refreshAuthorizationContext());
        if (!this.repository.canAdministerExecutingUnit(previousActiveExecutingUnitId)) {
          await this.failClosed('Saliste de Administración de usuarios porque la UE activa ya no tiene rol Administrador PIIP.');
          return;
        }
      }
      onReconciled?.();
      await this.load();
      this.snackBar.open(message, 'Cerrar', { duration: 2600 });
    } catch {
      await this.failClosed('La operación cambió, pero no fue posible actualizar tu acceso. Se cerró esta vista por seguridad.');
    } finally {
      releaseOperation();
    }
  }

  private async failClosed(message: string): Promise<void> {
    this.clearAdministrationView();
    await this.authorizationRecovery.enter(message, () => Promise.resolve(this.repository.initialize()));
    await this.router.navigateByUrl('/inicio');
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

  private showOperationError(error: unknown, fallback: string): void {
    const status = error instanceof HttpErrorResponse || error instanceof PiipApiError ? error.status : undefined;
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
