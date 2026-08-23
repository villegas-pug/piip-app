import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of } from 'rxjs';
import { resolveApiUrl } from '../../core/piip-http.repository';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { NewUserAssignmentDialogComponent } from './new-user-assignment-dialog.component';
import { SuspendUserAssignmentDialogComponent } from './suspend-user-assignment-dialog.component';
import { UserAdministrationComponent } from './user-administration.component';

describe('UserAdministrationComponent operations', () => {
  let http: HttpTestingController;
  let snackBar: Pick<MatSnackBar, 'open'>;
  let dialog: { open: ReturnType<typeof vi.fn> };
  const apiUrl = resolveApiUrl();

  beforeEach(async () => {
    snackBar = { open: vi.fn() };
    dialog = { open: vi.fn(() => ({ afterClosed: () => NEVER })) };
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await TestBed.configureTestingModule({
      imports: [UserAdministrationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: dialog }],
    }).overrideComponent(UserAdministrationComponent, {
      add: { providers: [{ provide: MatSnackBar, useValue: snackBar }] },
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const repository = TestBed.inject(PiipMockRepository);
    repository.currentUser.update((user) => user ? ({
      ...user,
      roleScopes: [
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 2, executingUnitId: 8 },
        { role: 'CONSULTA_EXTERNA', institutionId: 3, executingUnitId: 9 },
      ],
    }) : null);
    repository.administrableScopes.set([
      {
        institutionId: 1,
        institutionCode: 'INST-1',
        institutionName: 'Institución 1',
        institutionWideAllowed: true,
        executingUnits: [
          { id: 1, code: 'UE-001', name: 'UE-001' },
          { id: 2, code: 'UE-002', name: 'UE-002' },
        ],
      },
      {
        institutionId: 2,
        institutionCode: 'INST-2',
        institutionName: 'Institución 2',
        institutionWideAllowed: true,
        executingUnits: [{ id: 8, code: 'UE-008', name: 'UE-008' }],
      },
    ]);
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('prevents duplicate role assignments and suspensions and releases their states', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();
    fixture.componentInstance.assignmentForm.setValue({ userSubject: 'usuario-1', role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 });

    fixture.componentInstance.assign();
    fixture.componentInstance.assign();
    const assignment = http.expectOne(`${apiUrl}/admin/role-assignments`);
    expect(assignment.request.body).toEqual({ userSubject: 'usuario-1', role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 });
    assignment.flush(jsonBlob({}));
    expect(fixture.componentInstance.assigning()).toBe(false);
    flushAdministrationLoad(http, apiUrl);

    const scope = activeScope();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    fixture.componentInstance.suspend(scope);
    fixture.componentInstance.suspend(scope);
    expect(fixture.componentInstance.changingScopeId()).toBe(9);
    const suspension = http.expectOne(`${apiUrl}/admin/role-assignments/9?version=2`);
    expect(dialog.open).toHaveBeenCalledWith(SuspendUserAssignmentDialogComponent, expect.objectContaining({
      data: expect.objectContaining({ scope }),
    }));
    suspension.flush('');
    expect(fixture.componentInstance.changingScopeId()).toBeNull();
    flushAdministrationLoad(http, apiUrl);
  });

  it('opens the new assignment dialog with the existing typed form', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();

    fixture.componentInstance.openAssignment();

    expect(dialog.open).toHaveBeenCalledWith(NewUserAssignmentDialogComponent, expect.objectContaining({
      data: expect.objectContaining({ form: fixture.componentInstance.assignmentForm }),
    }));
  });

  it('joins first-assignment candidates by subject and identifies them without changing the table listing', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [
      { id: 1, subject: 'asignado', fullName: 'Usuario asignado', email: 'asignado@midagri.gob.pe', scopes: [] },
    ], [
      { id: 2, subject: 'candidato', fullName: 'Persona candidata', email: 'candidato@midagri.gob.pe' },
      { id: 1, subject: 'asignado', fullName: 'Usuario asignado', email: 'asignado@midagri.gob.pe' },
    ]);
    await fixture.whenStable();

    expect(fixture.componentInstance.assignmentRows()).toHaveLength(1);
    expect(fixture.componentInstance.assignmentUsers()).toEqual([
      { subject: 'asignado', fullName: 'Usuario asignado', email: 'asignado@midagri.gob.pe', withoutAssignments: true },
      { subject: 'candidato', fullName: 'Persona candidata', email: 'candidato@midagri.gob.pe', withoutAssignments: true },
    ]);
  });

  it('prevents an exact active assignment duplicate without an HTTP request, normalizing zero as no executing unit', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1, subject: 'usuario-1', fullName: 'Usuario 1', email: 'usuario1@midagri.gob.pe',
      scopes: [{ id: 9, role: 'ADMINISTRADOR_PIIP', institution: 'MIDAGRI', institutionId: 1, executingUnit: 'Toda la institución', executingUnitId: undefined, active: true, version: 1 }],
    }]);
    await fixture.whenStable();
    fixture.componentInstance.assignmentForm.setValue({ userSubject: 'usuario-1', role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 0 });

    fixture.componentInstance.assign();

    http.expectNone(`${apiUrl}/admin/role-assignments`);
    expect(snackBar.open).toHaveBeenLastCalledWith('El usuario ya cuenta con una asignación activa igual. Elige otro rol o ámbito.', 'Cerrar', { duration: 4200 });
  });

  it('edits role and scope with its version, then reactivates a suspended assignment', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();
    const scope = activeScope();

    fixture.componentInstance.openEdit(scope);
    fixture.componentInstance.editForm.setValue({ role: 'CONSULTA_EXTERNA', institutionId: 2, executingUnitId: 8 });
    fixture.componentInstance.saveEdit();
    const update = http.expectOne(`${apiUrl}/admin/role-assignments/9?version=2`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual({ role: 'CONSULTA_EXTERNA', institutionId: 2, executingUnitId: 8 });
    update.flush(jsonBlob({}));
    expect(fixture.componentInstance.editingScope()).toBeNull();
    flushAdministrationLoad(http, apiUrl);

    fixture.componentInstance.reactivate({ ...scope, active: false, version: 3 });
    const reactivation = http.expectOne(`${apiUrl}/admin/role-assignments/9/reactivation?version=3`);
    expect(reactivation.request.method).toBe('PUT');
    reactivation.flush(jsonBlob({}));
    flushAdministrationLoad(http, apiUrl);
  });

  it.each([
    [403, 'No tienes autorización para realizar esta operación.'],
    [409, 'La información cambió. Actualiza la pantalla antes de volver a intentarlo.'],
    [422, 'La operación no cumple las validaciones de rol, ámbito o cobertura.'],
  ])('informs the actionable message for assignment operations %i', async (status, message) => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();

    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    fixture.componentInstance.suspend(activeScope());
    http.expectOne(`${apiUrl}/admin/role-assignments/9?version=2`).flush({}, { status, statusText: 'Error' });
    await fixture.whenStable();

    expect(snackBar.open).toHaveBeenLastCalledWith(message, 'Cerrar', { duration: 4200 });
    expect(fixture.componentInstance.changingScopeId()).toBeNull();
  });

  it('does not mutate when the suspension dialog is cancelled', () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });

    fixture.componentInstance.suspend(activeScope());

    expect(dialog.open).toHaveBeenCalledWith(SuspendUserAssignmentDialogComponent, expect.any(Object));
    http.expectNone((request) => request.url.includes('/admin/role-assignments/'));
  });

  it('hydrates the active-user context after suspending and reactivating an own assignment', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 2, code: 'UE-002', name: 'Unidad administradora 2', institutionId: 1 },
      { id: 8, code: 'UE-008', name: 'Unidad administradora 8', institutionId: 2 },
    ]);
    repository.selectedExecutingUnitId.set(2);
    const refreshAuthorizationContext = vi.spyOn(repository, 'refreshAuthorizationContext');
    const scope = { ...activeScope(), executingUnitId: 2, executingUnit: 'UE-002' };
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'demo-admin',
      fullName: 'Administrador PIIP',
      email: 'admin.piip@midagri.gob.pe',
      scopes: [scope],
    }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });

    fixture.componentInstance.suspend(scope);

    expect(dialog.open).toHaveBeenCalledWith(SuspendUserAssignmentDialogComponent, expect.objectContaining({
      data: {
        scope,
        userName: 'Administrador PIIP',
        userEmail: 'admin.piip@midagri.gob.pe',
      },
    }));
    http.expectOne(`${apiUrl}/admin/role-assignments/9?version=2`).flush('');
    await fixture.whenStable();
    expect(refreshAuthorizationContext).toHaveBeenCalledOnce();
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'demo-admin',
      fullName: 'Administrador PIIP',
      email: 'admin.piip@midagri.gob.pe',
      scopes: [{ ...scope, active: false, version: 3 }],
    }]);

    fixture.componentInstance.reactivate({ ...scope, active: false, version: 3 });
    http.expectOne(`${apiUrl}/admin/role-assignments/9/reactivation?version=3`).flush(jsonBlob({}));
    await fixture.whenStable();
    expect(refreshAuthorizationContext).toHaveBeenCalledTimes(2);
    flushAdministrationLoad(http, apiUrl);
  });

  it('redirects when the original active UE disappears even if hydration falls back to another Administrator UE', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 2, code: 'UE-002', name: 'Unidad administradora 2', institutionId: 1 },
      { id: 8, code: 'UE-008', name: 'Unidad administradora 8', institutionId: 2 },
    ]);
    repository.selectedExecutingUnitId.set(2);
    const scope = { ...activeScope(), executingUnitId: 2, executingUnit: 'UE-002' };
    vi.spyOn(repository, 'refreshAuthorizationContext').mockImplementation(() => {
      repository.currentUser.update((user) => user ? ({
        ...user,
        roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 2, executingUnitId: 8 }],
      }) : null);
      repository.executingUnits.set([{ id: 8, code: 'UE-008', name: 'Unidad administradora 8', institutionId: 2 }]);
      repository.selectedExecutingUnitId.set(8);
    });
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'demo-admin',
      fullName: 'Administrador PIIP',
      email: 'admin.piip@midagri.gob.pe',
      scopes: [scope],
    }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });

    fixture.componentInstance.suspend(scope);
    http.expectOne(`${apiUrl}/admin/role-assignments/9?version=2`).flush('');
    await fixture.whenStable();

    expect(fixture.componentInstance.users()).toEqual([]);
    expect(repository.administrableScopes()).toEqual([]);
    expect(repository.selectedExecutingUnitId()).toBe(8);
    expect(navigateByUrl).toHaveBeenCalledWith('/inicio');
  });

  it('renders role and scope in ordered assignment rows with grouped accessible cells', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'usuario-multiple',
      fullName: 'Usuario múltiple',
      email: 'multiple@midagri.gob.pe',
      scopes: [
        { ...activeScope(), id: 10, role: 'CONSULTA_EXTERNA', executingUnit: 'UE-002', executingUnitId: 2 },
        { ...activeScope(), id: 11, role: 'ADMINISTRADOR_PIIP', executingUnit: 'Toda la institución', executingUnitId: undefined },
      ],
    }]);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const headers = Array.from(element.querySelectorAll('thead th'));
    const rows = Array.from(element.querySelectorAll('tbody > tr.user-row'));
    expect(headers.map((header) => header.textContent?.trim())).toEqual(['Usuario', 'Rol', 'Ámbito', 'Estado', 'Acciones']);
    expect(headers.every((header) => header.getAttribute('scope') === 'col')).toBe(true);
    expect(rows).toHaveLength(2);
    expect(rows.map((row) => row.querySelector('.role-value')?.textContent?.trim())).toEqual(['Consulta externa', 'Administrador PIIP']);
    expect(rows.map((row) => row.querySelector('.scope-chip')?.textContent?.trim())).toEqual(['UE-002', 'Toda la institución']);
    expect(rows.map((row) => row.textContent).join('')).not.toContain('·');
    expect(element.querySelectorAll('tbody > tr.user-row .user-cell')).toHaveLength(1);
    expect(element.querySelector('.user-cell')?.getAttribute('scope')).toBe('rowgroup');
    expect(element.querySelector('.user-cell')?.getAttribute('rowspan')).toBe('2');
    expect(element.querySelector('.group-status')?.getAttribute('rowspan')).toBe('2');
    expect(element.querySelector('.actions')?.getAttribute('rowspan')).toBe('2');
    expect(element.querySelector('.actions')?.textContent).toContain('Ver detalle');
    expect(rows[1]?.querySelector('.scope-chip')?.classList.contains('scope-chip--institution')).toBe(true);
  });

  it('separates role, scope and institution in the expanded assignment detail', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'usuario-detalle',
      fullName: 'Usuario detalle',
      email: 'detalle@midagri.gob.pe',
      scopes: [
        { ...activeScope(), id: 10 },
        { ...activeScope(), id: 11, role: 'CONSULTA_EXTERNA', institution: 'Institución 2', executingUnit: 'UE-008', executingUnitId: 8 },
      ],
    }]);
    await fixture.whenStable();
    fixture.componentInstance.toggleGroup('usuario-detalle');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const detailHeaders = Array.from(element.querySelectorAll('.assignment-detail-head span')).map((item) => item.textContent?.trim());
    const detailRows = Array.from(element.querySelectorAll('.assignment-detail'));
    expect(detailHeaders).toEqual(['Rol', 'Ámbito', 'Institución', 'Estado', 'Acciones']);
    expect(element.querySelector('.assignment-detail-row td')?.getAttribute('colspan')).toBe('5');
    expect(detailRows[0]?.querySelector('.role-value')?.textContent?.trim()).toBe('Administrador PIIP');
    expect(detailRows[0]?.querySelector('.scope-chip')?.textContent?.trim()).toBe('UE-001');
    expect(detailRows[0]?.querySelector('small')?.textContent?.trim()).toBe('MIDAGRI');
    expect(element.querySelector('.user-row .actions')?.textContent).toContain('Ocultar detalle');
  });

  it('shows separate role and scope fallbacks for a user without assignments', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{ id: 1, subject: 'sin-scope', fullName: 'Usuario sin scope', email: 'sin-scope@midagri.gob.pe', scopes: [] }]);
    await fixture.whenStable();
    fixture.detectChanges();

    const row = (fixture.nativeElement as HTMLElement).querySelector('tbody > tr.user-row');
    expect(row?.querySelector('.role-cell')?.textContent?.trim()).toBe('Sin rol asignado');
    expect(row?.querySelector('.scope-cell')?.textContent?.trim()).toBe('Sin ámbito asignado');
  });

  it('paginates users as grouped assignments', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    const users = Array.from({ length: 6 }, (_, index) => ({
      id: index + 1, subject: `usuario-${index + 1}`, fullName: `Usuario ${index + 1}`, email: `usuario${index + 1}@midagri.gob.pe`,
      scopes: [1, 2].map((scope) => ({ id: index * 10 + scope, role: 'CONSULTA_EXTERNA' as const, institution: 'MIDAGRI', institutionId: 1, executingUnit: `UE-00${scope}`, active: true, version: 1 })),
    }));
    http.expectOne(`${apiUrl}/admin/users`).flush(jsonBlob(users));
    http.expectOne(`${apiUrl}/admin/users/assignment-candidates`).flush([]);
    await fixture.whenStable();

    expect(fixture.componentInstance.assignmentRows()).toHaveLength(12);
    expect(fixture.componentInstance.assignmentGroups()).toHaveLength(6);
    expect(fixture.componentInstance.pagedAssignmentGroups()).toHaveLength(5);
    fixture.componentInstance.pageIndex.set(1);
    expect(fixture.componentInstance.pagedAssignmentGroups()).toEqual([expect.objectContaining({ user: expect.objectContaining({ fullName: 'Usuario 6' }) })]);
  });

  it('shows the loading state and accessible empty state after an empty generated listing', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    expect(fixture.componentInstance.loading()).toBe(true);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    const emptyState = fixture.nativeElement.querySelector('.empty-state');
    expect(emptyState?.getAttribute('role')).toBe('status');
    expect(emptyState?.textContent).toContain('No hay usuarios ni asignaciones disponibles');
  });

  it('presents only the assignment state and no account management action', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    http.expectOne(`${apiUrl}/admin/users`).flush(jsonBlob([{ id: 7, subject: 'usuario', fullName: 'Usuario', email: 'usuario@midagri.gob.pe', scopes: [{ ...activeScope(), active: false }] }]));
    http.expectOne(`${apiUrl}/admin/users/assignment-candidates`).flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const states = Array.from(fixture.nativeElement.querySelectorAll('.status') as NodeListOf<Element>).map((item) => item.textContent?.trim());
    expect(states).toEqual(['1 suspendida']);
    expect(fixture.nativeElement.textContent).not.toContain('Inhabilitar usuario');
    expect(fixture.nativeElement.textContent).not.toContain('Habilitar usuario');
  });

  it('uses only the administrative catalog for institutions, all active units and institution-wide scope', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();

    expect(fixture.componentInstance.administrableInstitutions().map((item) => item.id)).toEqual([1, 2]);
    expect(fixture.componentInstance.canUseInstitutionWide(1)).toBe(true);
    expect(fixture.componentInstance.canUseInstitutionWide(2)).toBe(true);
    fixture.componentInstance.assignmentForm.controls.institutionId.setValue(1);
    expect(fixture.componentInstance.assignmentExecutingUnits().map((item) => item.id)).toEqual([1, 2]);
  });

  it('allows assigning the current user within the administrative catalog', async () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'demo-admin',
      fullName: 'Administrador PIIP',
      email: 'admin.piip@midagri.gob.pe',
      scopes: [],
    }]);
    await fixture.whenStable();
    fixture.componentInstance.assignmentForm.setValue({
      userSubject: 'demo-admin',
      role: 'CONSULTA_EXTERNA',
      institutionId: 1,
      executingUnitId: 1,
    });

    fixture.componentInstance.assign();

    const assignment = http.expectOne(`${apiUrl}/admin/role-assignments`);
    expect(assignment.request.body).toEqual({
      userSubject: 'demo-admin',
      role: 'CONSULTA_EXTERNA',
      institutionId: 1,
      executingUnitId: 1,
    });
    assignment.flush(jsonBlob({}));
    flushAdministrationLoad(http, apiUrl);
  });

  it('does not filter the authorized catalog or table when another Administrator UE becomes active', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 2, code: 'UE-002', name: 'Unidad administradora 2', institutionId: 1 },
      { id: 8, code: 'UE-008', name: 'Unidad administradora 8', institutionId: 2 },
    ]);
    repository.selectedExecutingUnitId.set(2);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1,
      subject: 'usuario',
      fullName: 'Usuario transversal',
      email: 'usuario@example.pe',
      scopes: [activeScope(), { ...activeScope(), id: 10, institutionId: 2, executingUnitId: 8 }],
    }]);
    await fixture.whenStable();

    repository.selectExecutingUnit(8);
    fixture.componentInstance.assignmentForm.controls.institutionId.setValue(1);

    expect(fixture.componentInstance.assignmentRows()).toHaveLength(2);
    expect(fixture.componentInstance.administrableInstitutions().map((item) => item.id)).toEqual([1, 2]);
    expect(fixture.componentInstance.assignmentExecutingUnits().map((item) => item.id)).toEqual([1, 2]);
  });

  it('summarizes the active Administrator UE while keeping assignments from every administrable scope', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 2, code: 'UE-002', name: 'Unidad activa', institutionId: 1 },
      { id: 8, code: 'UE-008', name: 'Otra unidad administrable', institutionId: 2 },
    ]);
    repository.selectedExecutingUnitId.set(2);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl, [{
      id: 1, subject: 'usuario', fullName: 'Usuario transversal', email: 'usuario@example.pe',
      scopes: [
        activeScope(),
        { ...activeScope(), id: 10, institutionId: 2, institution: 'Institución 2', executingUnitId: 8, executingUnit: 'UE-008' },
      ],
    }]);
    await fixture.whenStable();
    fixture.detectChanges();

    const context = (fixture.nativeElement as HTMLElement).querySelector('.administration-summary');
    expect(context?.textContent).toContain('UE activa');
    expect(context?.textContent).toContain('UE-002');
    expect(context?.textContent).toContain('¿Cómo funciona este acceso?');
    expect(fixture.componentInstance.assignmentRows()).toHaveLength(2);
  });

  it('requires explicit confirmation before assigning an institution-wide role', async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();
    fixture.componentInstance.assignmentForm.setValue({ userSubject: 'usuario-1', role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 0 });

    fixture.componentInstance.assign();

    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('toda la institución'));
    http.expectNone(`${apiUrl}/admin/role-assignments`);
  });

  it('requires explicit confirmation before changing an assignment to institution-wide', async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    await fixture.whenStable();
    fixture.componentInstance.openEdit(activeScope());
    fixture.componentInstance.editForm.setValue({ role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 0 });

    fixture.componentInstance.saveEdit();

    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('toda la institución'));
    http.expectNone(`${apiUrl}/admin/role-assignments/9?version=2`);
  });
});

function activeScope() {
  return { id: 9, role: 'ADMINISTRADOR_PIIP' as const, institution: 'MIDAGRI', institutionId: 1, executingUnit: 'UE-001', executingUnitId: 1, active: true, version: 2 };
}

function flushAdministrationLoad(http: HttpTestingController, apiUrl: string, users: unknown[] = [], candidates: unknown[] = []): void {
  http.expectOne(`${apiUrl}/admin/users`).flush(jsonBlob(users));
  http.expectOne(`${apiUrl}/admin/users/assignment-candidates`).flush(candidates);
}

function jsonBlob(value: unknown): Blob {
  return new Blob([JSON.stringify(value)], { type: 'application/json' });
}
