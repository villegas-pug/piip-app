export interface PiipRuntimeConfig {
  apiUrl: string;
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
}

declare global {
  interface Window {
    __PIIP_RUNTIME_CONFIG__?: Partial<PiipRuntimeConfig>;
  }
}

const LOCAL_API_URL = 'http://127.0.0.1:4001/api/v1';

export function resolveRuntimeConfig(): PiipRuntimeConfig {
  const configured = typeof window === 'undefined' ? undefined : window.__PIIP_RUNTIME_CONFIG__;
  const keycloak = configured?.keycloak;
  if (!keycloak?.url?.trim() || !keycloak.realm?.trim() || !keycloak.clientId?.trim()) {
    throw new Error('Falta configurar URL, realm o client ID de Keycloak.');
  }

  return {
    apiUrl: resolveApiUrl(),
    keycloak: {
      url: keycloak.url.trim(),
      realm: keycloak.realm.trim(),
      clientId: keycloak.clientId.trim(),
    },
  };
}

export function resolveApiUrl(): string {
  const configured = typeof window === 'undefined' ? undefined : window.__PIIP_RUNTIME_CONFIG__;
  return (configured?.apiUrl?.trim() || LOCAL_API_URL).replace(/\/$/, '');
}
