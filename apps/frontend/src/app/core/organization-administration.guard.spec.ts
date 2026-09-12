import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PiipMockRepository } from './piip-mock.repository';
import { PIIP_REPOSITORY } from './piip-repository.token';
import { organizationAdministrationGuard } from './organization-administration.guard';

describe('organizationAdministrationGuard', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatSnackBar, useValue: { open: vi.fn() } }],
  }));

  it('allows an administrator grant without requiring the global active UE', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.selectedExecutingUnitId.set(null);
    expect(await TestBed.runInInjectionContext(() => organizationAdministrationGuard(null!, { url: '/administracion/unidades-ejecutoras' } as never))).toBe(true);
  });

  it('redirects users without an Administrator PIIP grant', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.toggleRole();
    const result = await TestBed.runInInjectionContext(() => organizationAdministrationGuard(null!, { url: '/administracion/unidades-ejecutoras' } as never));
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toContain('/inicio');
  });
});
