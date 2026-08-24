import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipApiError } from '../../core/piip-http.repository';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { AuthorizationRecoveryService } from '../../core/authorization-recovery.service';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import type { UserAdministrationSnapshot, UserAssignmentScope } from '../../core/piip.models';
import { NewUserAssignmentDialogComponent } from './new-user-assignment-dialog.component';
import { UserAdministrationComponent } from './user-administration.component';

describe('UserAdministrationComponent', () => {
  let repository: PiipMockRepository;
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sessionStorage.removeItem('piip-authorization-recovery');
    snackBar = { open: vi.fn() };
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(null) })) };
    await TestBed.configureTestingModule({
      imports: [UserAdministrationComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: dialog }],
    }).overrideComponent(UserAdministrationComponent, {
      add: { providers: [{ provide: MatSnackBar, useValue: snackBar }] },
    }).compileComponents();
    repository = TestBed.inject(PiipMockRepository);
    repository.currentUser.update((user) => user ? ({ ...user, roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }] }) : null);
    repository.selectedExecutingUnitId.set(1);
  });

  afterEach(() => {
    sessionStorage.removeItem('piip-authorization-recovery');
    vi.restoreAllMocks();
  });

  it('carga usuarios y candidatos por PIIP_REPOSITORY y abre el diálogo de alta', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    expect(fixture.componentInstance.assignmentUsers()).toEqual(expect.arrayContaining([
      expect.objectContaining({ subject: 'demo-admin', withoutAssignments: false }),
      expect.objectContaining({ subject: 'demo-consulta', withoutAssignments: true }),
    ]));
    fixture.componentInstance.openAssignment();
    expect(dialog.open).toHaveBeenCalledWith(NewUserAssignmentDialogComponent, expect.objectContaining({ data: expect.objectContaining({ form: fixture.componentInstance.assignmentForm }) }));
  });

  it('envía la edición con ID y versión y solo recarga después de confirmar la respuesta', async () => {
    const scope = assignmentScope({ id: 21, role: 'CONSULTA_EXTERNA', version: 3 });
    const snapshot = snapshotWithScope(scope);
    repository.userAdministrationUsers.set(snapshot.users);
    vi.spyOn(repository, 'updateUserAssignment').mockResolvedValue({ outcome: 'UPDATED', status: 200, scope: { ...scope, role: 'ADMINISTRADOR_PIIP', version: 4 } });
    vi.spyOn(repository, 'loadUserAdministration').mockResolvedValue({ users: [{ ...snapshot.users[0], scopes: [{ ...scope, role: 'ADMINISTRADOR_PIIP', version: 4 }] }], assignmentCandidates: [] });
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    dialog.open.mockReturnValue({ afterClosed: () => of({ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }) });
    fixture.componentInstance.openEdit(scope);
    fixture.componentInstance.editForm.setValue({ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 });
    fixture.componentInstance.saveEdit();
    expect(fixture.componentInstance.savingScopeId()).toBe(21);
    await settle();
    expect(repository.updateUserAssignment).toHaveBeenCalledWith(21, 3, { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 });
    expect(fixture.componentInstance.savingScopeId()).toBeNull();
    expect(fixture.componentInstance.editingScope()).toBeNull();
  });

  it('suspende una asignación mediante el repositorio y conserva el estado bloqueado hasta recargar', async () => {
    const scope = assignmentScope({ id: 31, role: 'CONSULTA_EXTERNA', version: 2 });
    repository.userAdministrationUsers.set(snapshotWithScope(scope).users);
    vi.spyOn(repository, 'suspendUserAssignment').mockResolvedValue({ outcome: 'SUSPENDED', status: 204, scope: { ...scope, active: false, version: 3 } });
    vi.spyOn(repository, 'loadUserAdministration').mockResolvedValue({ users: snapshotWithScope({ ...scope, active: false, version: 3 }).users, assignmentCandidates: [] });
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    fixture.componentInstance.suspend(scope);
    await settle();
    expect(repository.suspendUserAssignment).toHaveBeenCalledWith(31, 2);
    expect(fixture.componentInstance.changingScopeId()).toBeNull();
  });

  it('refresca la autorización antes de reconciliar una edición y reactivación propias', async () => {
    const subject = repository.currentUser()!.subject;
    const editable = assignmentScope({ id: 35, role: 'CONSULTA_EXTERNA', version: 2 });
    repository.userAdministrationUsers.set([{ id: 10, subject, fullName: 'Administrador', email: 'admin@example.pe', scopes: [editable] }]);
    vi.spyOn(repository, 'updateUserAssignment').mockResolvedValue({ outcome: 'UPDATED', status: 200, scope: { ...editable, version: 3 } });
    vi.spyOn(repository, 'reactivateUserAssignment').mockResolvedValue({ outcome: 'REACTIVATED', status: 200, scope: { ...editable, active: true, version: 4 } });
    vi.spyOn(repository, 'refreshAuthorizationContext').mockResolvedValue();
    vi.spyOn(repository, 'loadUserAdministration').mockResolvedValue({ users: [{ id: 10, subject, fullName: 'Administrador', email: 'admin@example.pe', scopes: [editable] }], assignmentCandidates: [] });
    vi.spyOn(repository, 'loadAdministrableScopes').mockResolvedValue();
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();

    dialog.open.mockReturnValue({ afterClosed: () => of({ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }) });
    fixture.componentInstance.openEdit(editable);
    fixture.componentInstance.saveEdit();
    await settle();
    expect(repository.updateUserAssignment).toHaveBeenCalledWith(35, 2, { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 });

    fixture.componentInstance.reactivate({ ...editable, active: false, version: 3 });
    await settle();
    expect(repository.reactivateUserAssignment).toHaveBeenCalledWith(35, 3);
    expect(repository.refreshAuthorizationContext).toHaveBeenCalledTimes(2);
  });

  it('mantiene visible pero no ejecutable la autosuspensión de Administrador PIIP propio', async () => {
    const scope = assignmentScope({ id: 41, role: 'ADMINISTRADOR_PIIP' });
    repository.currentUser.update((user) => user ? { ...user, subject: 'external-user' } : null);
    repository.userAdministrationUsers.set(snapshotWithScope(scope).users);
    const suspend = vi.spyOn(repository, 'suspendUserAssignment');
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    expect(fixture.componentInstance.isSelfAdministrator(scope)).toBe(true);
    fixture.componentInstance.suspend(scope);
    expect(suspend).not.toHaveBeenCalled();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se puede suspender la propia asignación Administrador PIIP');
  });

  it('muestra el fallback accionable ante un rechazo de asignación', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    fixture.componentInstance.assignmentForm.setValue({ userSubject: 'demo-consulta', role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 });
    vi.spyOn(repository, 'assignUserRole').mockRejectedValue(new PiipApiError(409, 'detalle no estable', 'ACTIVE_ASSIGNMENT_DUPLICATE'));
    fixture.componentInstance.assign();
    await settle();
    expect(snackBar.open).toHaveBeenCalledWith('La información cambió. Actualiza la pantalla antes de volver a intentarlo.', 'Cerrar', { duration: 4200 });
    expect(fixture.componentInstance.assigning()).toBe(false);
  });

  it.each([
    ['CREATED', 'Rol asignado.'],
    ['REACTIVATED', 'Asignación reactivada.'],
  ] as const)('coordina una mutación propia de alta y distingue el resultado %s', async (outcome, message) => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    const subject = repository.currentUser()!.subject;
    vi.spyOn(repository, 'assignUserRole').mockResolvedValue({
      outcome,
      status: outcome === 'CREATED' ? 201 : 200,
      scope: assignmentScope({ id: outcome === 'CREATED' ? 61 : 62, role: 'CONSULTA_EXTERNA', version: 2 }),
    });
    vi.spyOn(repository, 'refreshAuthorizationContext').mockResolvedValue();
    vi.spyOn(repository, 'loadUserAdministration').mockResolvedValue({ users: [], assignmentCandidates: [] });
    vi.spyOn(repository, 'loadAdministrableScopes').mockResolvedValue();
    fixture.componentInstance.assignmentForm.setValue({ userSubject: subject, role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 });

    fixture.componentInstance.assign();
    expect(fixture.componentInstance.operationPending()).toBe(true);
    await settle();

    expect(repository.assignUserRole).toHaveBeenCalledWith({ userSubject: subject, role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 });
    expect(repository.refreshAuthorizationContext).toHaveBeenCalledOnce();
    expect(snackBar.open).toHaveBeenCalledWith(message, 'Cerrar', { duration: 2600 });
    expect(fixture.componentInstance.operationPending()).toBe(false);
  });

  it('cierra en fail-closed y deja recovery persistente si falla la rehidratación propia', async () => {
    const scope = assignmentScope({ id: 51, role: 'CONSULTA_EXTERNA' });
    repository.currentUser.update((user) => user ? { ...user, subject: 'external-user' } : null);
    repository.userAdministrationUsers.set(snapshotWithScope(scope).users);
    vi.spyOn(repository, 'suspendUserAssignment').mockResolvedValue({ outcome: 'SUSPENDED', status: 204, scope: { ...scope, active: false, version: 2 } });
    vi.spyOn(repository, 'refreshAuthorizationContext').mockRejectedValue(new Error('no disponible'));
    const navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const recovery = TestBed.inject(AuthorizationRecoveryService);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    await settle();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    fixture.componentInstance.suspend(scope);
    await settle();
    expect(fixture.componentInstance.users()).toEqual([]);
    expect(recovery.active()).toBe(true);
    expect(navigateByUrl).toHaveBeenCalledWith('/inicio');
    expect(sessionStorage.getItem('piip-authorization-recovery')).toContain('asignación cambió');
  });
});

function assignmentScope(overrides: Partial<UserAssignmentScope> = {}): UserAssignmentScope {
  return { id: 1, role: 'CONSULTA_EXTERNA', institutionId: 1, institution: 'Institución de demostración', executingUnitId: 1, executingUnit: 'Unidad Ejecutora de demostración', active: true, version: 1, ...overrides };
}

function snapshotWithScope(scope: UserAssignmentScope): UserAdministrationSnapshot {
  return { users: [{ id: 10, subject: 'external-user', fullName: 'Usuario externo', email: 'external@example.pe', scopes: [scope] }], assignmentCandidates: [] };
}

async function settle(): Promise<void> {
  await Promise.resolve();
  await new Promise((resolve) => setTimeout(resolve, 0));
}
