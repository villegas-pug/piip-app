import { resolveApiUrl, resolveRuntimeConfig } from './piip-runtime-config';

describe('PIIP runtime configuration', () => {
  afterEach(() => {
    delete window.__PIIP_RUNTIME_CONFIG__;
  });

  it('requires Keycloak in every environment', () => {
    window.__PIIP_RUNTIME_CONFIG__ = { apiUrl: 'http://127.0.0.1:4001/api/v1' };

    expect(() => resolveRuntimeConfig()).toThrowError(
      'Falta configurar URL, realm o client ID de Keycloak.',
    );
  });

  it('normalizes the configured public values', () => {
    window.__PIIP_RUNTIME_CONFIG__ = {
      apiUrl: ' https://api.example.gob.pe/api/v1/ ',
      keycloak: {
        url: ' https://identity.example.gob.pe ',
        realm: ' piip ',
        clientId: ' piip-web ',
      },
    };

    expect(resolveRuntimeConfig()).toEqual({
      apiUrl: 'https://api.example.gob.pe/api/v1',
      keycloak: { url: 'https://identity.example.gob.pe', realm: 'piip', clientId: 'piip-web' },
    });
  });

  it('keeps the local API default independent from authentication configuration', () => {
    delete window.__PIIP_RUNTIME_CONFIG__;

    expect(resolveApiUrl()).toBe('http://127.0.0.1:4001/api/v1');
  });
});
