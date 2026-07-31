import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { normalizeInternalReturnUrl, PiipAuthService } from './piip-auth.service';

export const authenticatedGuard: CanActivateFn = (_route, state) => {
  const auth = inject(PiipAuthService);
  if (auth.ready() && auth.authenticated()) return true;

  return inject(Router).createUrlTree(['/login'], {
    queryParams: { returnUrl: normalizeInternalReturnUrl(state.url) },
  });
};
