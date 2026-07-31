import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { resolveApiUrl } from '../../core/piip-http.repository';
import { UserAdministrationComponent } from './user-administration.component';

describe('UserAdministrationComponent operations', () => {
  let http: HttpTestingController;
  const apiUrl = resolveApiUrl();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAdministrationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: MatSnackBar, useValue: { open: vi.fn() } }],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('prevents duplicate role assignments and suspensions and releases their states', () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    flushAdministrationLoad(http, apiUrl);
    fixture.componentInstance.assignmentForm.setValue({
      userSubject: 'usuario-1', role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1,
    });

    fixture.componentInstance.assign();
    fixture.componentInstance.assign();
    expect(fixture.componentInstance.assigning()).toBe(true);
    http.expectOne(`${apiUrl}/admin/role-assignments`).flush({});
    expect(fixture.componentInstance.assigning()).toBe(false);
    flushAdministrationLoad(http, apiUrl);

    const scope = { id: 9, role: 'ADMINISTRADOR_PIIP' as const, institution: 'MIDAGRI', executingUnit: 'UE-001', active: true, version: 2 };
    fixture.componentInstance.suspend(scope);
    fixture.componentInstance.suspend(scope);
    expect(fixture.componentInstance.suspendingScopeId()).toBe(9);
    http.expectOne((request) => request.url === `${apiUrl}/admin/role-assignments/9`).flush({});
    expect(fixture.componentInstance.suspendingScopeId()).toBeNull();
    flushAdministrationLoad(http, apiUrl);
  });

  it('paginates user scopes as identified rows', () => {
    const fixture = TestBed.createComponent(UserAdministrationComponent);
    const users = Array.from({ length: 3 }, (_, index) => ({
      id: index + 1,
      subject: `usuario-${index + 1}`,
      fullName: `Usuario ${index + 1}`,
      email: `usuario${index + 1}@midagri.gob.pe`,
      active: true,
      scopes: [1, 2].map((scope) => ({ id: index * 10 + scope, role: 'CONSULTA_EXTERNA' as const, institution: 'MIDAGRI', executingUnit: `UE-00${scope}`, active: true, version: 1 })),
    }));
    http.expectOne(`${apiUrl}/admin/users`).flush(users);
    http.expectOne(`${apiUrl}/institutions`).flush([]);
    http.expectOne(`${apiUrl}/executing-units`).flush([]);

    expect(fixture.componentInstance.assignmentRows()).toHaveLength(6);
    expect(fixture.componentInstance.pagedAssignmentRows()).toHaveLength(5);
    fixture.componentInstance.pageIndex.set(1);
    expect(fixture.componentInstance.pagedAssignmentRows()).toEqual([expect.objectContaining({ user: expect.objectContaining({ fullName: 'Usuario 3' }) })]);
  });
});

function flushAdministrationLoad(http: HttpTestingController, apiUrl: string): void {
  http.expectOne(`${apiUrl}/admin/users`).flush([]);
  http.expectOne(`${apiUrl}/institutions`).flush([]);
  http.expectOne(`${apiUrl}/executing-units`).flush([]);
}
