import { routes } from './app.routes';

describe('organization administration routes', () => {
  it('declares separate UE and UO list and form paths', () => {
    const children = routes.find((route) => route.path === '')?.children ?? [];
    expect(children.map((route) => route.path)).toEqual(expect.arrayContaining([
      'administracion/unidades-ejecutoras',
      'administracion/unidades-ejecutoras/nueva',
      'administracion/unidades-ejecutoras/:id/editar',
      'administracion/unidades-ejecutoras/:executingUnitId/unidades-organicas',
      'administracion/unidades-ejecutoras/:executingUnitId/unidades-organicas/nueva',
      'administracion/unidades-ejecutoras/:executingUnitId/unidades-organicas/:id/editar',
    ]));
  });
});
