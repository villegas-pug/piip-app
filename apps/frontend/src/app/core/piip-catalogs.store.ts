import { Injectable, signal } from '@angular/core';
import { CatalogBundle, OrganizationalUnit, ResourceState } from './piip.models';

const EMPTY_BUNDLE: CatalogBundle = {
  recordTypes: [], solutionTypes: [], sources: [], peiObjectives: [], poiActivities: [], documentTypes: [],
};

@Injectable({ providedIn: 'root' })
export class PiipCatalogsStore {
  readonly catalogs = signal<ResourceState<CatalogBundle>>(idle(EMPTY_BUNDLE));
  readonly organizationalUnits = signal<ResourceState<OrganizationalUnit[]>>(idle([]));
  private catalogRequestId = 0;
  private organizationalUnitRequestId = 0;

  async loadCatalogs(loader: () => Promise<CatalogBundle>): Promise<void> {
    const requestId = ++this.catalogRequestId;
    this.catalogs.set({ phase: 'loading', value: this.catalogs().value, error: null, requestId });
    try {
      const value = await loader();
      if (requestId === this.catalogRequestId) this.catalogs.set({ phase: 'ready', value, error: null, requestId });
    } catch (error) {
      if (requestId === this.catalogRequestId) this.catalogs.set({ phase: 'error', value: this.catalogs().value, error: message(error, 'No fue posible cargar los catálogos.'), requestId });
    }
  }

  async loadOrganizationalUnits(executingUnitId: number, loader: () => Promise<OrganizationalUnit[]>): Promise<void> {
    const requestId = ++this.organizationalUnitRequestId;
    this.organizationalUnits.set({ phase: 'loading', value: [], error: null, requestId });
    try {
      const value = (await loader()).filter((unit) => unit.active && unit.executingUnitId === executingUnitId);
      if (requestId === this.organizationalUnitRequestId) this.organizationalUnits.set({ phase: 'ready', value, error: null, requestId });
    } catch (error) {
      if (requestId === this.organizationalUnitRequestId) this.organizationalUnits.set({ phase: 'error', value: [], error: message(error, 'No fue posible cargar las Unidades Orgánicas.'), requestId });
    }
  }

  clearOrganizationalUnits(): void {
    ++this.organizationalUnitRequestId;
    this.organizationalUnits.set(idle([]));
  }
}

function idle<T>(value: T): ResourceState<T> {
  return { phase: 'idle', value, error: null, requestId: 0 };
}

function message(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}
