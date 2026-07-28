// Sustituir en el despliegue institucional sin recompilar Angular.
window.__PIIP_RUNTIME_CONFIG__ = {
  apiUrl: 'http://localhost:4001/api/v1',
  authenticationRequired: true,
  keycloak: {
    url: 'https://rcgv-services-dev.duckdns.org/keycloak',
    realm: 'piip',
    clientId: 'rovidev-client',
  },
};
