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
});
