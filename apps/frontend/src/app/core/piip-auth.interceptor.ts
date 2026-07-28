import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { PiipAuthService } from './piip-auth.service';
import { resolveApiUrl } from './piip-runtime-config';

export const piipAuthInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(resolveApiUrl())) return next(request);
  const auth = inject(PiipAuthService);
  return from(auth.validToken()).pipe(
    switchMap((token) => next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request)),
  );
};
