import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import {
  PIIP_KEYCLOAK_CLIENT_FACTORY,
  PiipAuthService,
  normalizeInternalReturnUrl,
} from './piip-auth.service';

describe('PiipAuthService', () => {
  let auth: PiipAuthService;
  let keycloak: Keycloak;
  let init: ReturnType<typeof vi.fn>;
  let login: ReturnType<typeof vi.fn>;
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    window.__PIIP_RUNTIME_CONFIG__ = {
      apiUrl: 'http://127.0.0.1:4001/api/v1',
      keycloak: { url: 'https://identity.example.gob.pe', realm: 'piip', clientId: 'piip-web' },
    };
    sessionStorage.clear();

    init = vi.fn().mockResolvedValue(false);
    login = vi.fn().mockResolvedValue(undefined);
    logout = vi.fn().mockResolvedValue(undefined);
    keycloak = {
      init,
      login,
      logout,
      updateToken: vi.fn().mockResolvedValue(false),
      token: 'access-token',
    } as unknown as Keycloak;

    TestBed.configureTestingModule({
      providers: [
        PiipAuthService,
        { provide: PIIP_KEYCLOAK_CLIENT_FACTORY, useValue: () => keycloak },
      ],
    });
    auth = TestBed.inject(PiipAuthService);
  });

  afterEach(() => {
    delete window.__PIIP_RUNTIME_CONFIG__;
    sessionStorage.clear();
  });

  it('checks the existing SSO session with Authorization Code and PKCE', async () => {
    init.mockResolvedValue(true);

    await auth.initialize();

    expect(init).toHaveBeenCalledWith({
      onLoad: 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      checkLoginIframe: true,
    });
    expect(auth.ready()).toBe(true);
    expect(auth.authenticated()).toBe(true);
  });

  it('reports a blocking error when Keycloak is not configured', async () => {
    delete window.__PIIP_RUNTIME_CONFIG__;

    await auth.initialize();

    expect(init).not.toHaveBeenCalled();
    expect(auth.authenticated()).toBe(false);
    expect(auth.configurationError()).toContain('Falta configurar');
  });

  it('reports a blocking error when the identity service is unavailable', async () => {
    init.mockRejectedValue(new Error('Network error'));

    await auth.initialize();

    expect(auth.authenticated()).toBe(false);
    expect(auth.configurationError()).toContain('No fue posible conectar');
  });

  it('stores a safe internal route and uses the fixed login callback', async () => {
    await auth.initialize();

    await auth.login('/auditoria?tab=accesos');

    expect(login).toHaveBeenCalledWith({
      redirectUri: new URL('/login', window.location.origin).href,
    });
    expect(auth.consumePostLoginRoute()).toBe('/auditoria?tab=accesos');
    expect(auth.consumePostLoginRoute()).toBe('/inicio');
  });

  it('replaces external and login callback returns with the default route', () => {
    expect(normalizeInternalReturnUrl('https://malicious.example/steal')).toBe('/inicio');
    expect(normalizeInternalReturnUrl('//malicious.example/steal')).toBe('/inicio');
    expect(normalizeInternalReturnUrl('/login')).toBe('/inicio');
  });

  it('clears the pending route and returns to login when logging out', async () => {
    init.mockResolvedValue(true);
    await auth.initialize();
    await auth.login('/auditoria');

    await auth.logout();

    expect(auth.authenticated()).toBe(false);
    expect(auth.consumePostLoginRoute()).toBe('/inicio');
    expect(logout).toHaveBeenCalledWith({
      redirectUri: new URL('/login', window.location.origin).href,
    });
  });
});
