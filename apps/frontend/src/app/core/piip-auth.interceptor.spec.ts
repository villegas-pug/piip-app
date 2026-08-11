import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { piipAuthInterceptor } from './piip-auth.interceptor';
import { PiipAuthService } from './piip-auth.service';

describe('piipAuthInterceptor', () => {
  const validToken = vi.fn();
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    window.__PIIP_RUNTIME_CONFIG__ = {
      apiUrl: 'https://api.example.gob.pe/api/v1',
      keycloak: { url: 'https://identity.example.gob.pe', realm: 'piip', clientId: 'piip-web' },
    };
    validToken.mockReset().mockResolvedValue('access-token');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([piipAuthInterceptor])),
        provideHttpClientTesting(),
        { provide: PiipAuthService, useValue: { validToken } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    delete window.__PIIP_RUNTIME_CONFIG__;
  });

  it('adds the bearer token only to the PIIP API', async () => {
    http.get('https://api.example.gob.pe/api/v1/identity/me').subscribe();
    await Promise.resolve();

    const request = controller.expectOne('https://api.example.gob.pe/api/v1/identity/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({});
  });

  it('does not request or expose the token for external URLs', () => {
    http.get('https://cdn.example.org/public.json').subscribe();

    const request = controller.expectOne('https://cdn.example.org/public.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(validToken).not.toHaveBeenCalled();
    request.flush({});
  });
});
