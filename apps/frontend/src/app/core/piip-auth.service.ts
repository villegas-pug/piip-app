import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { resolveRuntimeConfig } from './piip-runtime-config';

@Injectable({ providedIn: 'root' })
export class PiipAuthService {
  readonly authenticated = signal(false);
  readonly ready = signal(false);
  readonly configurationError = signal<string | null>(null);
  private keycloak?: Keycloak;

  async initialize(): Promise<void> {
    let config;
    try {
      config = resolveRuntimeConfig();
    } catch (error) {
      this.configurationError.set(error instanceof Error ? error.message : 'Configuracion de autenticacion invalida.');
      this.ready.set(true);
      return;
    }

    if (!config.authenticationRequired) {
      this.ready.set(true);
      return;
    }
    if (!config.keycloak?.url || !config.keycloak.realm || !config.keycloak.clientId) {
      this.configurationError.set('Falta configurar URL, realm o client ID de Keycloak.');
      this.ready.set(true);
      return;
    }

    this.keycloak = new Keycloak(config.keycloak);
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        flow: 'standard',
        pkceMethod: 'S256',
        checkLoginIframe: true,
      });
      this.authenticated.set(authenticated);
      this.keycloak.onTokenExpired = () => void this.refreshToken();
    } catch {
      this.configurationError.set('No fue posible iniciar sesion con Keycloak.');
    } finally {
      this.ready.set(true);
    }
  }

  async validToken(): Promise<string | undefined> {
    if (!this.keycloak || !this.authenticated()) return undefined;
    await this.refreshToken();
    return this.keycloak.token;
  }

  async login(): Promise<void> {
    await this.keycloak?.login({ redirectUri: window.location.href });
  }

  async logout(): Promise<void> {
    if (!this.keycloak) return;
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  private async refreshToken(): Promise<void> {
    if (!this.keycloak) return;
    try {
      await this.keycloak.updateToken(30);
    } catch {
      this.authenticated.set(false);
      await this.login();
    }
  }
}
