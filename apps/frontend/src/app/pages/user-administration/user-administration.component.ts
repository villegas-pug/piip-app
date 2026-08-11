import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, finalize, forkJoin, from, map, of, switchMap } from 'rxjs';
import type { Observable } from 'rxjs';
import { UserAdministrationControllerService } from '../../api/generated';
import type { ScopeResponse, UserAssignmentCandidateResponse, UserResponse } from '../../api/generated';
import { PiipApiError, resolveApiUrl } from '../../core/piip-http.repository';
import type { AdministrableExecutingUnit } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';

type UserScope = ScopeResponse;
type UserItem = UserResponse;
type AssignmentCandidate = UserAssignmentCandidateResponse;
type Role = NonNullable<UserScope['role']>;
interface UserAssignmentRow { user: UserItem; scope: UserScope | null; }
interface AssignmentUser { subject: string; fullName: string; email: string; withoutAssignments: boolean; }
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
  private readonly apiUrl = resolveApiUrl();
  readonly repository = inject(PIIP_REPOSITORY);

  readonly users = signal<UserItem[]>([]);
  readonly assignmentCandidates = signal<AssignmentCandidate[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly assignmentOpen = signal(false);
  readonly assigning = signal(false);
  readonly editingScope = signal<UserScope | null>(null);
  readonly savingScopeId = signal<number | null>(null);
  readonly changingScopeId = signal<number | null>(null);
  readonly pageIndex = signal(0);
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
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.assignmentRows().length));
  readonly pagedAssignmentRows = computed(() => paginateItems(this.assignmentRows(), this.currentPage()));
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
  }

  cancelEdit(): void {
    if (!this.operationPending()) this.editingScope.set(null);
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
    if (!window.confirm('¿Deseas suspender esta asignación? El acceso quedará retirado hasta que se reactive.')) return;
    this.changingScopeId.set(scope.id);
    this.userAdministration.suspend({ scopeId: scope.id, version: scope.version })
      .pipe(finalize(() => this.changingScopeId.set(null)))
      .subscribe({
        next: () => { this.snackBar.open('Asignación suspendida.', 'Cerrar', { duration: 2600 }); this.load(); },
        error: (response) => this.showOperationError(response, 'No se pudo suspender la asignación.'),
      });
  }

  reactivate(scope: UserScope): void {
    if (this.operationPending() || scope.id === undefined || scope.version === undefined || scope.active) return;
    this.changingScopeId.set(scope.id);
    this.userAdministration.reactivate({ scopeId: scope.id, version: scope.version })
      .pipe(finalize(() => this.changingScopeId.set(null)))
      .subscribe({
        next: () => { this.snackBar.open('Asignación reactivada.', 'Cerrar', { duration: 2600 }); this.load(); },
        error: (response) => this.showOperationError(response, 'No se pudo reactivar la asignación.'),
      });
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
        next: () => { this.assignmentOpen.set(false); this.snackBar.open('Rol asignado.', 'Cerrar', { duration: 2600 }); this.load(); },
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

  editExecutingUnits(): AdministrableExecutingUnit[] {
    return this.administrableExecutingUnits(this.editForm.controls.institutionId.value);
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
