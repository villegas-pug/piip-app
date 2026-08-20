import { describe, expect, it } from 'vitest';
import { PiipCatalogsStore } from './piip-catalogs.store';
import { CatalogBundle, OrganizationalUnit } from './piip.models';

const EMPTY: CatalogBundle = { recordTypes: [], solutionTypes: [], sources: [], peiObjectives: [], poiActivities: [], documentTypes: [] };

describe('PiipCatalogsStore', () => {
  it('distingue carga, respuesta vacía, error y reintento', async () => {
    const store = new PiipCatalogsStore();
    const first = store.loadCatalogs(async () => EMPTY);
    expect(store.catalogs().phase).toBe('loading');
    await first;
    expect(store.catalogs()).toMatchObject({ phase: 'ready', value: EMPTY, error: null });

    await store.loadCatalogs(async () => { throw new Error('Catálogo no disponible'); });
    expect(store.catalogs()).toMatchObject({ phase: 'error', error: 'Catálogo no disponible' });

    await store.loadCatalogs(async () => ({ ...EMPTY, sources: [option(1, 'SOURCE', 'Fuente')] }));
    expect(store.catalogs().phase).toBe('ready');
    expect(store.catalogs().value.sources[0]?.id).toBe(1);
  });

  it('descarta la respuesta tardía de una Unidad Ejecutora anterior', async () => {
    const store = new PiipCatalogsStore();
    let resolveFirst!: (value: OrganizationalUnit[]) => void;
    const first = store.loadOrganizationalUnits(1, () => new Promise((resolve) => { resolveFirst = resolve; }));
    const secondUnit = unit(2, 2);
    await store.loadOrganizationalUnits(2, async () => [secondUnit]);
    resolveFirst([unit(1, 1)]);
    await first;
    expect(store.organizationalUnits().value).toEqual([secondUnit]);
  });

  it('solo conserva opciones activas de la Unidad Ejecutora solicitada', async () => {
    const store = new PiipCatalogsStore();
    await store.loadOrganizationalUnits(1, async () => [{ ...unit(1, 1), active: false }, unit(2, 2), unit(3, 1)]);
    expect(store.organizationalUnits().value.map((item) => item.id)).toEqual([3]);
  });

  it('conserva la identidad por ID aunque una opción sea renombrada', async () => {
    const store = new PiipCatalogsStore();
    const selectedId = 7;
    await store.loadCatalogs(async () => ({ ...EMPTY, sources: [option(selectedId, 'SOURCE', 'Nombre anterior')] }));
    await store.loadCatalogs(async () => ({ ...EMPTY, sources: [option(selectedId, 'SOURCE', 'Nombre vigente')] }));

    expect(store.catalogs().value.sources.find((item) => item.id === selectedId)).toMatchObject({
      id: selectedId,
      name: 'Nombre vigente',
      active: true,
    });
  });

  it('expone una referencia inactiva por su mismo ID para reconciliar una selección histórica', async () => {
    const store = new PiipCatalogsStore();
    const selectedId = 9;
    await store.loadCatalogs(async () => ({ ...EMPTY, peiObjectives: [option(selectedId, 'PEI', 'Objetivo vigente')] }));
    await store.loadCatalogs(async () => ({
      ...EMPTY,
      peiObjectives: [{ ...option(selectedId, 'PEI', 'Objetivo histórico'), active: false }],
    }));

    expect(store.catalogs().value.peiObjectives.find((item) => item.id === selectedId)).toEqual(
      expect.objectContaining({ id: selectedId, name: 'Objetivo histórico', active: false }),
    );
  });

  it('descarta una respuesta global tardía y conserva la recarga más reciente', async () => {
    const store = new PiipCatalogsStore();
    let resolveFirst!: (value: CatalogBundle) => void;
    const first = store.loadCatalogs(() => new Promise((resolve) => { resolveFirst = resolve; }));
    await store.loadCatalogs(async () => ({ ...EMPTY, sources: [option(2, 'NEW', 'Respuesta vigente')] }));
    resolveFirst({ ...EMPTY, sources: [option(1, 'OLD', 'Respuesta tardía')] });
    await first;

    expect(store.catalogs().value.sources.map((item) => item.id)).toEqual([2]);
  });
});

function option(id: number, code: string, name: string) {
  return { id, code, name, displayOrder: id, active: true };
}

function unit(id: number, executingUnitId: number): OrganizationalUnit {
  return { id, executingUnitId, code: `UO-${id}`, name: `Unidad ${id}`, acronym: `U${id}`, parentId: null, active: true };
}
