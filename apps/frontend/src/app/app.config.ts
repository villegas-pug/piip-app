import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { PiipHttpRepository } from './core/piip-http.repository';
import { piipAuthInterceptor } from './core/piip-auth.interceptor';
import { PIIP_REPOSITORY } from './core/piip-repository.token';
import { routes } from './app.routes';
import { PiipAuthService } from './core/piip-auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAppInitializer(() => inject(PiipAuthService).initialize()),
    provideHttpClient(withInterceptors([piipAuthInterceptor])),
    provideRouter(routes, withViewTransitions()),
    PiipHttpRepository,
    { provide: PIIP_REPOSITORY, useExisting: PiipHttpRepository },
  ],
};
