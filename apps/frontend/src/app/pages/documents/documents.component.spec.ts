import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DocumentFile } from '../../core/piip.models';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentsComponent } from './documents.component';

describe('DocumentsComponent archivos independientes', () => {
  const dialog = { open: vi.fn() };

  beforeEach(async () => {
    dialog.open.mockReset();
    const paramMap = convertToParamMap({ code: 'I-024-2026' });
    await TestBed.configureTestingModule({
      imports: [DocumentsComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { paramMap: of(paramMap), data: of({ recordType: 'Iniciativa' }), snapshot: { paramMap, data: { recordType: 'Iniciativa' } } } },
      ],
    }).compileComponents();
  });

  it('presenta archivos independientes con su vigente e historial por archivo', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const dossier = repository.documentDossiers()[0];
    const record = dossier.stages[0].records[0];
    const first: DocumentFile = {
      id: 901,
      original: true,
      latestVersion: 1,
      current: { id: 9011, number: 1, filename: 'anexo-a-v1.pdf', uploadedAt: '01/09/2026 08:30', externallyPublished: false, optimisticVersion: 0 },
      versions: [
        { id: 9011, number: 1, filename: 'anexo-a-v1.pdf', uploadedAt: '01/09/2026 08:30', externallyPublished: false, optimisticVersion: 0 },
      ],
    };
    const second: DocumentFile = {
      id: 902,
      original: false,
      latestVersion: 2,
      current: { id: 9022, number: 2, filename: 'anexo-b-v2.pdf', uploadedAt: '01/09/2026 10:30', externallyPublished: true, optimisticVersion: 1 },
      versions: [
        { id: 9022, number: 2, filename: 'anexo-b-v2.pdf', uploadedAt: '01/09/2026 10:30', externallyPublished: true, optimisticVersion: 1 },
        { id: 9021, number: 1, filename: 'anexo-b-v1.pdf', uploadedAt: '01/09/2026 09:30', externallyPublished: false, optimisticVersion: 0 },
      ],
    };
    repository.documentDossiers.set([{ ...dossier, stages: [{ ...dossier.stages[0], records: [{ ...record, files: [first, second] }] }, ...dossier.stages.slice(1)] }, ...repository.documentDossiers().slice(1)]);
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    const documentRow = host.querySelector<HTMLElement>('.document-row');
    expect(documentRow?.querySelectorAll('.document-file')).toHaveLength(2);
    expect(host.textContent).toContain('anexo-b-v2.pdf');
    expect(host.textContent).not.toContain('anexo-b-v1.pdf');
    expect(host.textContent).toContain('versión vigente 2');
    fixture.componentInstance.toggleFileHistory(second);
    fixture.detectChanges();
    expect(host.textContent).toContain('anexo-b-v1.pdf');
    expect(host.querySelector('[aria-label="Descargar versión 1 de anexo-b-v1.pdf"]')).not.toBeNull();
  });

  it('muestra el nombre de la iniciativa en el banner del expediente de un proyecto derivado', () => {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        paramMap: of(convertToParamMap({ code: 'P-005-2026' })),
        data: of({ recordType: 'Proyecto' }),
        snapshot: {
          paramMap: convertToParamMap({ code: 'P-005-2026' }),
          data: { recordType: 'Proyecto' },
        },
      },
    });
    const repository = TestBed.inject(PiipMockRepository);
    repository.projects.update((projects) => projects.map((project) => project.code === 'P-005-2026'
      ? { ...project, originCode: 'I-019-2026', originMode: 'DERIVED_FROM_INITIATIVE' }
      : project));
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.origin-document-banner') as HTMLElement;

    expect(banner.textContent).toContain('Fortalecimiento de capacidades para la gestión de la innovación agraria');
    expect(banner.textContent).not.toContain('I-019-2026');
    expect(banner.querySelector('a[href="/iniciativas/I-019-2026/documentos"]')).not.toBeNull();
  });

  it('distingue Agregar archivo de Nueva versión y conserva un selector de archivo por operación', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const addFile = vi.spyOn(repository, 'addDocumentFile').mockImplementation(() => Promise.resolve() as unknown as void);
    const addVersion = vi.spyOn(repository, 'addDocumentFileVersion').mockImplementation(() => Promise.resolve() as unknown as void);
    const fixture = TestBed.createComponent(DocumentsComponent);
    const component = fixture.componentInstance;
    const document = component.dossier()!.stages[0].records[0];
    const target = document.files![0];
    const file = new File(['contenido'], 'nuevo.pdf', { type: 'application/pdf' });

    component.openAddFilePanel();
    component.uploadType.set(document.documentTypeId!);
    component.uploadFile.set(file);
    await component.upload();
    expect(addFile).toHaveBeenCalledWith('I-024-2026', document.documentTypeId, file);
    expect(addVersion).not.toHaveBeenCalled();

    component.openNewVersionPanel(document, target);
    component.uploadFile.set(file);
    await component.upload();
    expect(addVersion).toHaveBeenCalledWith('I-024-2026', target.id, file);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('input[type="file"]')).toHaveLength(0);
  });

  it('opera una nueva versión exclusivamente sobre el fileId seleccionado', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DocumentsComponent);
    const component = fixture.componentInstance;
    const document = component.dossier()!.stages[0].records[0];
    const original = document.files![0];
    repository.addDocumentFile('I-024-2026', document.documentTypeId!, new File(['b'], 'b.pdf', { type: 'application/pdf' }));
    const target = repository.getDocumentDossier('Iniciativa', 'I-024-2026')!.stages[0].records[0].files!.find((file) => file.id !== original.id)!;

    component.openNewVersionPanel(document, target);
    component.uploadFile.set(new File(['b2'], 'b-v2.pdf', { type: 'application/pdf' }));
    await component.upload();
    const files = repository.getDocumentDossier('Iniciativa', 'I-024-2026')!.stages[0].records[0].files!;
    expect(files.find((file) => file.id === target.id)?.versions).toHaveLength(2);
    expect(files.find((file) => file.id === original.id)?.versions).toHaveLength(1);
  });

  it('abre un diálogo antes de eliminar y elimina solo el archivo confirmado', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DocumentsComponent);
    const component = fixture.componentInstance;
    const document = component.dossier()!.stages[0].records[0];
    repository.addDocumentFile('I-024-2026', document.documentTypeId!, new File(['b'], 'b.pdf', { type: 'application/pdf' }));
    const files = repository.getDocumentDossier('Iniciativa', 'I-024-2026')!.stages[0].records[0].files!;
    const [target, survivor] = files;
    const deleteFile = vi.spyOn(repository, 'deleteDocumentFile');
    dialog.open.mockReturnValueOnce({ afterClosed: () => of(false) }).mockReturnValueOnce({ afterClosed: () => of(true) });

    await component.requestDeleteFile(document, target);
    expect(deleteFile).not.toHaveBeenCalled();
    await component.requestDeleteFile(document, target);
    expect(dialog.open).toHaveBeenCalledTimes(2);
    expect(deleteFile).toHaveBeenCalledWith('I-024-2026', target.id);
    expect(repository.getDocumentDossier('Iniciativa', 'I-024-2026')!.stages[0].records[0].files?.map((file) => file.id)).toEqual([survivor.id]);
  });

  it('mantiene las guardas accesibles y oculta las escrituras a Consulta externa', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.toggleRole();
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.textContent).not.toContain('Agregar archivo');
    expect(host.textContent).not.toContain('Nueva versión');
    expect(host.textContent).not.toContain('Eliminar archivo');
    expect(host.querySelector('[aria-label^="Descargar versión"]')).not.toBeNull();
  });

  it('expone el formato permitido y el estado ocupado del panel de agregar archivo', () => {
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.componentInstance.openAddFilePanel();
    fixture.componentInstance.pendingOperation.set({ kind: 'add-file', key: '40' });
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.document-upload-panel')?.getAttribute('aria-busy')).toBe('true');
    expect(host.querySelector<HTMLInputElement>('.file-picker-input')?.accept).toBe('.pdf,.docx,.xlsx');
    expect(host.querySelector<HTMLInputElement>('.file-picker-input')?.disabled).toBe(true);
  });
});
