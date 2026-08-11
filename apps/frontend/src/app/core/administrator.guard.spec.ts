import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { PiipMockRepository } from './piip-mock.repository';
import { PIIP_REPOSITORY } from './piip-repository.token';
import { administratorGuard } from './administrator.guard';

describe('administratorGuard', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
  }));

  it('allows transversal administration even when the active UE only has Consulta externa', async () => {
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
    repository.selectedExecutingUnitId.set(1);

    expect(await TestBed.runInInjectionContext(() => administratorGuard(null!, null!))).toBe(true);
  });

  it('redirects a user without any Administrator grant', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.toggleRole();
    const result = await TestBed.runInInjectionContext(() => administratorGuard(null!, null!));
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/inicio');
  });
});
