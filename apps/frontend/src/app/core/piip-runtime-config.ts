export interface PiipRuntimeConfig {
  apiUrl: string;
  authenticationRequired: boolean;
  keycloak?: {
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

const LOCAL_DEFAULTS: PiipRuntimeConfig = {
  apiUrl: 'http://127.0.0.1:4001/api/v1',
  authenticationRequired: false,
};

export function resolveRuntimeConfig(): PiipRuntimeConfig {
  const configured = typeof window === 'undefined' ? undefined : window.__PIIP_RUNTIME_CONFIG__;
  const config: PiipRuntimeConfig = {
    ...LOCAL_DEFAULTS,
    ...configured,
    keycloak: configured?.keycloak,
  };

  const hostname = typeof location === 'undefined' ? 'localhost' : location.hostname;
  const isLocal = hostname === 'localhost' || hostname === '127.0.0.1';
  if (!isLocal && !config.authenticationRequired) {
    throw new Error('La autenticacion Keycloak es obligatoria fuera del entorno local.');
  }
  return config;
}

export function resolveApiUrl(): string {
  return resolveRuntimeConfig().apiUrl.replace(/\/$/, '');
}
