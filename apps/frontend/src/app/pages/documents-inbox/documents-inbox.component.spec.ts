import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentsInboxComponent } from './documents-inbox.component';

describe('DocumentsInboxComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DocumentsInboxComponent], providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }] }).compileComponents();
  });

  it('filters the global inbox by record type and search text', () => {
    const fixture = TestBed.createComponent(DocumentsInboxComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.filteredDossiers().some((dossier) => dossier.recordType === 'Iniciativa')).toBe(true);
    expect(component.filteredDossiers().some((dossier) => dossier.recordType === 'Proyecto')).toBe(true);

    component.filters.patchValue({ recordType: 'Proyecto', search: 'P-005' });
    fixture.detectChanges();

    expect(component.filteredDossiers()).toHaveLength(1);
    expect(component.filteredDossiers()[0].code).toBe('P-005-2026');
  });

  it('paginates filtered dossiers and resets to the first page', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.documentDossiers()[0];
    repository.documentDossiers.set(Array.from({ length: 6 }, (_, index) => ({ ...template, code: `I-20${index}-2026`, name: `Expediente ${index + 1}` })));
    const fixture = TestBed.createComponent(DocumentsInboxComponent);
    const component = fixture.componentInstance;

    expect(component.pagedDossiers()).toHaveLength(5);
    component.pageIndex.set(1);
    expect(component.pagedDossiers()).toHaveLength(1);

    component.filters.patchValue({ search: 'I-200-2026' });
    expect(component.currentPage()).toBe(0);
  });

  it('mantiene exclusivamente búsqueda, Tipo de registro, estado y Unidad Orgánica', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const target = { ...repository.getDocumentDossierSummaries()[0], organizationalUnits: [repository.organizationalUnits()[0]] };
    vi.spyOn(repository, 'getDocumentDossierSummaries').mockReturnValue([target]);
    const fixture = TestBed.createComponent(DocumentsInboxComponent);
    fixture.componentInstance.filters.patchValue({
      search: target.code,
      recordType: target.recordType,
      status: target.status,
      unit: String(target.organizationalUnits[0]?.id),
    });

    expect(Object.keys(fixture.componentInstance.filters.getRawValue()).sort()).toEqual(['recordType', 'search', 'status', 'unit']);
    expect(fixture.componentInstance.filteredDossiers().map((item) => item.code)).toEqual([target.code]);
  });

  it('presenta carga, vacío y error para las opciones de catálogo', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DocumentsInboxComponent);
    repository.catalogs.set({ phase: 'loading', value: repository.catalogs().value, error: null, requestId: 2 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Cargando opciones de filtro');

    repository.organizationalUnits.set([]);
    repository.catalogs.set({ phase: 'ready', value: { ...repository.catalogs().value, recordTypes: [] }, error: null, requestId: 3 });
    repository.organizationalUnitsState.set({ phase: 'ready', value: [], error: null, requestId: 3 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay opciones activas');

    repository.catalogs.set({ phase: 'error', value: repository.catalogs().value, error: 'Catálogo no disponible', requestId: 4 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Catálogo no disponible');
  });
});
