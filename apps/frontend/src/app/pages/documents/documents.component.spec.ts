import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentRecord } from '../../core/piip.models';
import { DocumentsComponent } from './documents.component';

describe('DocumentsComponent operations', () => {
  beforeEach(async () => {
    const paramMap = convertToParamMap({ code: 'I-024-2026' });
    await TestBed.configureTestingModule({
      imports: [DocumentsComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: { paramMap: of(paramMap), data: of({ recordType: 'Iniciativa' }), snapshot: { paramMap, data: { recordType: 'Iniciativa' } } } },
      ],
    }).compileComponents();
  });

  it('identifies the active document action and prevents duplicate downloads', async () => {
    const repository = TestBed.inject(PIIP_REPOSITORY);
    let releaseDownload!: () => void;
    const pendingDownload = new Promise<void>((resolve) => { releaseDownload = resolve; });
    const downloadDocument = vi.spyOn(repository, 'downloadDocument').mockReturnValue(pendingDownload);
    const fixture = TestBed.createComponent(DocumentsComponent);
    const documentRecord: DocumentRecord = {
      type: 'PUBLIC_INNOVATION_INITIATIVE_SHEET', name: 'Ficha', required: true,
      filename: 'ficha.pdf', version: '1.0', versionId: 25, uploadedAt: '31/07/2026', state: 'Cargado',
    };

    const first = fixture.componentInstance.download(documentRecord);
    const duplicate = fixture.componentInstance.download(documentRecord);

    expect(downloadDocument).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.isPending('download', documentRecord)).toBe(true);
    releaseDownload();
    await Promise.all([first, duplicate]);
    expect(fixture.componentInstance.operationPending()).toBe(false);
  });
});
