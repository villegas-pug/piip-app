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
});
