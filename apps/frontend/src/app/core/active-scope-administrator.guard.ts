import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PIIP_REPOSITORY } from './piip-repository.token';

export const activeScopeAdministratorGuard: CanActivateFn = async (_route, state) => {
  const repository = inject(PIIP_REPOSITORY);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  await Promise.resolve(repository.initialize());
  if (repository.canAdministerExecutingUnit(repository.selectedExecutingUnitId())) return true;
  if (state?.url.startsWith('/administracion/usuarios')) {
    snackBar.open(
      'Selecciona una Unidad Ejecutora donde tengas el rol Administrador PIIP para administrar usuarios.',
      'Cerrar',
      { duration: 5200 },
    );
  }
  return router.createUrlTree(['/inicio']);
};
