import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { PiipActivityService } from './piip-activity.service';
import { resolveApiUrl } from './piip-runtime-config';

export const piipLoadingInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(resolveApiUrl())) return next(request);

  const finishRequest = inject(PiipActivityService).beginRequest();
  return next(request).pipe(finalize(finishRequest));
};
