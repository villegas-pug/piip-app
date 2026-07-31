import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { PiipAuthService } from '../../core/piip-auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  const authenticated = signal(false);
  const configurationError = signal<string | null>(null);
  const login = vi.fn();
  const consumePostLoginRoute = vi.fn();

  beforeEach(async () => {
    authenticated.set(false);
    configurationError.set(null);
    login.mockReset().mockResolvedValue(undefined);
    consumePostLoginRoute.mockReset().mockReturnValue('/inicio');

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        {
          provide: PiipAuthService,
          useValue: { authenticated, configurationError, login, consumePostLoginRoute },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ returnUrl: '/auditoria' }) } },
        },
      ],
    }).compileComponents();
  });

  it('presents access before the institutional content without credential fields', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const accessCard = fixture.nativeElement.querySelector('.access-card');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Acceso a PIIP');
    expect(fixture.nativeElement.querySelector('.hero-copy h2')?.textContent).toContain(
      'Gestión de Iniciativas',
    );
    expect(accessCard?.nextElementSibling?.classList.contains('hero-copy')).toBe(true);
    expect(fixture.nativeElement.querySelector('input')).toBeNull();
    expect(fixture.nativeElement.querySelector('.login-button')?.textContent).toContain(
      'Ingresar con cuenta institucional',
    );
  });

  it('prevents duplicate redirects while login is pending', () => {
    login.mockReturnValue(new Promise<void>(() => undefined));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    void fixture.componentInstance.login();
    void fixture.componentInstance.login();
    fixture.detectChanges();

    expect(login).toHaveBeenCalledOnce();
    expect(login).toHaveBeenCalledWith('/auditoria');
    expect(fixture.componentInstance.redirecting()).toBe(true);
    expect(fixture.nativeElement.querySelector('.login-button')?.textContent).toContain(
      'Conectando con el acceso institucional',
    );
  });

  it('shows the configuration error and does not offer login', () => {
    configurationError.set('Falta configurar Keycloak.');
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain(
      'Falta configurar Keycloak',
    );
    expect(fixture.nativeElement.querySelector('.login-button')).toBeNull();
    expect(fixture.nativeElement.querySelector('.retry-button')).not.toBeNull();
  });

  it('returns an authenticated session to the pending internal route', () => {
    authenticated.set(true);
    consumePostLoginRoute.mockReturnValue('/auditoria');
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginComponent);

    fixture.detectChanges();

    expect(consumePostLoginRoute).toHaveBeenCalledWith('/auditoria');
    expect(navigateByUrl).toHaveBeenCalledWith('/auditoria', { replaceUrl: true });
  });
});
