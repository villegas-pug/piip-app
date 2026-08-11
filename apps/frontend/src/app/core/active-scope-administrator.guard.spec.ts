import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { activeScopeAdministratorGuard } from './active-scope-administrator.guard';
import { PiipMockRepository } from './piip-mock.repository';
import { PIIP_REPOSITORY } from './piip-repository.token';

describe('activeScopeAdministratorGuard', () => {
  const open = vi.fn();

  beforeEach(() => {
    open.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open } },
      ],
    });
  });

  it('rejects creation in a Consulta externa UE even with an Administrator grant elsewhere', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'UE-001', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'UE-002', institutionId: 1 },
    ]);
    repository.currentUser.set({
      subject: 'mixed', fullName: 'Usuario mixto', email: 'mixed@example.pe',
      roleScopes: [
        { role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
      ], roles: ['CONSULTA_EXTERNA', 'ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1, 2], institutionWide: false,
    });
    repository.administrableScopes.set([{
      institutionId: 1,
      institutionCode: 'MIDAGRI',
      institutionName: 'Ministerio de Desarrollo Agrario y Riego',
      institutionWideAllowed: true,
      executingUnits: [
        { id: 1, code: 'UE-001', name: 'Unidad de consulta' },
        { id: 2, code: 'UE-002', name: 'Unidad administradora' },
      ],
    }]);
    repository.selectedExecutingUnitId.set(1);

    const result = await TestBed.runInInjectionContext(() => activeScopeAdministratorGuard(null!, { url: '/administracion/usuarios' } as RouterStateSnapshot));
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/inicio');
    expect(open).toHaveBeenCalledWith(
      'Selecciona una Unidad Ejecutora donde tengas el rol Administrador PIIP para administrar usuarios.',
      'Cerrar',
      { duration: 5200 },
    );

    repository.selectedExecutingUnitId.set(2);
    expect(await TestBed.runInInjectionContext(() => activeScopeAdministratorGuard(null!, null!))).toBe(true);
  });
});
