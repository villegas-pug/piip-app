import { isNavigationRouteActive } from './app-shell.component';

describe('PIIP shell navigation', () => {
  it('keeps Documentos active in the inbox and contextual dossier routes', () => {
    expect(isNavigationRouteActive('/documentos', '/documentos')).toBe(true);
    expect(isNavigationRouteActive('/documentos', '/iniciativas/I-024-2026/documentos')).toBe(true);
    expect(isNavigationRouteActive('/documentos', '/proyectos/P-005-2026/documentos?tab=gestion')).toBe(true);
  });

  it('does not activate Iniciativas or Proyectos inside a document dossier', () => {
    expect(isNavigationRouteActive('/iniciativas', '/iniciativas/I-024-2026/documentos')).toBe(false);
    expect(isNavigationRouteActive('/proyectos', '/proyectos/P-005-2026/documentos')).toBe(false);
  });
});
