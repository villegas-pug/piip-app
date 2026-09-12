import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PIIP_REPOSITORY } from './piip-repository.token';

/**
 * Guardia defensiva de UI: la autorización efectiva continúa en el backend.
 * A diferencia de la administración de usuarios, no depende de la UE global activa.
 */
export const organizationAdministrationGuard: CanActivateFn = async (_route, state) => {
  const repository = inject(PIIP_REPOSITORY);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  await Promise.resolve(repository.initialize());
  if (repository.hasAnyAdministratorScope()) return true;

  snackBar.open('La administración organizacional requiere el rol Administrador PIIP.', 'Cerrar', { duration: 4200 });
  return router.createUrlTree(['/inicio'], { queryParams: { returnUrl: state.url } });
};
