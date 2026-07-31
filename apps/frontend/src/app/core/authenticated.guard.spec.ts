import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { authenticatedGuard } from './authenticated.guard';
import { PiipAuthService } from './piip-auth.service';

describe('authenticatedGuard', () => {
  const authenticated = signal(false);
  const ready = signal(true);

  beforeEach(() => {
    authenticated.set(false);
    ready.set(true);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: PiipAuthService, useValue: { authenticated, ready } },
      ],
    });
  });

  it('redirects an anonymous user to login and preserves the internal route', () => {
    const result = runGuard('/auditoria?tab=eventos') as UrlTree;

    expect(result.queryParams['returnUrl']).toBe('/auditoria?tab=eventos');
    expect(TestBed.inject(Router).serializeUrl(result).startsWith('/login?')).toBe(true);
  });

  it('allows authenticated users into the protected shell', () => {
    authenticated.set(true);

    expect(runGuard('/inicio')).toBe(true);
  });

  it('does not preserve an external return URL', () => {
    const result = runGuard('//malicious.example/steal') as UrlTree;

    expect(result.queryParams['returnUrl']).toBe('/inicio');
  });

  function runGuard(url: string): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      authenticatedGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }
});
