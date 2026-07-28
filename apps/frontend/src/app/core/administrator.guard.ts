import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PIIP_REPOSITORY } from './piip-repository.token';

export const administratorGuard: CanActivateFn = async () => {
  const repository = inject(PIIP_REPOSITORY);
  const router = inject(Router);
  await Promise.resolve(repository.initialize());
  return repository.role() === 'Administrador PIIP' ? true : router.createUrlTree(['/inicio']);
};
