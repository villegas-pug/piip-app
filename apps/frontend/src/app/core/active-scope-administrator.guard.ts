import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PIIP_REPOSITORY } from './piip-repository.token';

export const activeScopeAdministratorGuard: CanActivateFn = async () => {
  const repository = inject(PIIP_REPOSITORY);
  const router = inject(Router);
  await Promise.resolve(repository.initialize());
  return repository.canAdministerExecutingUnit(repository.selectedExecutingUnitId())
    ? true
    : router.createUrlTree(['/inicio']);
};
