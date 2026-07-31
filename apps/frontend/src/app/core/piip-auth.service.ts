import { inject, Injectable, InjectionToken, signal } from '@angular/core';
import Keycloak, { type KeycloakConfig } from 'keycloak-js';
import { resolveRuntimeConfig } from './piip-runtime-config';

const DEFAULT_AUTHENTICATED_ROUTE = '/inicio';
const LOGIN_CALLBACK_ROUTE = '/login';
const POST_LOGIN_ROUTE_KEY = 'piip.postLoginRoute';

type KeycloakClientFactory = (config: KeycloakConfig) => Keycloak;

export const PIIP_KEYCLOAK_CLIENT_FACTORY = new InjectionToken<KeycloakClientFactory>(
  'PIIP_KEYCLOAK_CLIENT_FACTORY',
  {
    providedIn: 'root',
    factory: () => (config) => new Keycloak(config),
  },
);

export function normalizeInternalReturnUrl(returnUrl?: string | null): string {
  const candidate = returnUrl?.trim();
  if (
    !candidate ||
    !candidate.startsWith('/') ||
    candidate.startsWith('//') ||
    candidate.includes('\\')
  ) {
    return DEFAULT_AUTHENTICATED_ROUTE;
  }

  try {
    const resolved = new URL(candidate, window.location.origin);
    if (resolved.origin !== window.location.origin || resolved.pathname === LOGIN_CALLBACK_ROUTE) {
      return DEFAULT_AUTHENTICATED_ROUTE;
    }
    return `${resolved.pathname}${resolved.search}${resolved.hash}`;
  } catch {
    return DEFAULT_AUTHENTICATED_ROUTE;
  }
}

@Injectable({ providedIn: 'root' })
export class PiipAuthService {
  readonly authenticated = signal(false);
  readonly ready = signal(false);
  readonly configurationError = signal<string | null>(null);

  private readonly createKeycloakClient = inject(PIIP_KEYCLOAK_CLIENT_FACTORY);
  private keycloak?: Keycloak;

  async initialize(): Promise<void> {
    this.ready.set(false);
    this.configurationError.set(null);

    try {
      const config = resolveRuntimeConfig();
      this.keycloak = this.createKeycloakClient(config.keycloak);
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        flow: 'standard',
        pkceMethod: 'S256',
        checkLoginIframe: true,
      });

      this.authenticated.set(authenticated);
      this.keycloak.onTokenExpired = () => void this.refreshToken();
      this.keycloak.onAuthLogout = () => {
        this.authenticated.set(false);
        window.location.replace(this.loginCallbackUrl());
      };
    } catch (error) {
      this.authenticated.set(false);
      this.configurationError.set(
        error instanceof Error && error.message.startsWith('Falta configurar')
          ? error.message
          : 'No fue posible conectar con el servicio de autenticación Keycloak.',
      );
    } finally {
      this.ready.set(true);
    }
  }

  async validToken(): Promise<string | undefined> {
    if (!this.keycloak || !this.authenticated()) return undefined;
    await this.refreshToken();
    return this.keycloak.token;
  }

  async login(returnUrl?: string): Promise<void> {
    if (!this.keycloak) {
      throw new Error('Keycloak no está disponible para iniciar sesión.');
    }

    this.storePostLoginRoute(normalizeInternalReturnUrl(returnUrl));
    await this.keycloak.login({ redirectUri: this.loginCallbackUrl() });
  }

  consumePostLoginRoute(fallback?: string | null): string {
    let storedRoute: string | null = null;
    try {
      storedRoute = sessionStorage.getItem(POST_LOGIN_ROUTE_KEY);
      sessionStorage.removeItem(POST_LOGIN_ROUTE_KEY);
    } catch {
      // El almacenamiento puede estar deshabilitado; el retorno validado sigue siendo seguro.
    }
    return normalizeInternalReturnUrl(storedRoute ?? fallback);
  }

  async logout(): Promise<void> {
    this.clearPostLoginRoute();
    this.authenticated.set(false);
    if (!this.keycloak) return;
    await this.keycloak.logout({ redirectUri: this.loginCallbackUrl() });
  }

  private storePostLoginRoute(returnUrl: string): void {
    try {
      sessionStorage.setItem(POST_LOGIN_ROUTE_KEY, returnUrl);
    } catch {
      // Keycloak puede continuar; después del callback se usará /inicio.
    }
  }

  private clearPostLoginRoute(): void {
    try {
      sessionStorage.removeItem(POST_LOGIN_ROUTE_KEY);
    } catch {
      // No hay estado local que limpiar cuando el almacenamiento está deshabilitado.
    }
  }

  private loginCallbackUrl(): string {
    return new URL(LOGIN_CALLBACK_ROUTE, window.location.origin).href;
  }

  private async refreshToken(): Promise<void> {
    if (!this.keycloak) return;
    try {
      await this.keycloak.updateToken(30);
    } catch {
      this.authenticated.set(false);
      await this.login(
        `${window.location.pathname}${window.location.search}${window.location.hash}`,
      );
    }
  }
}
