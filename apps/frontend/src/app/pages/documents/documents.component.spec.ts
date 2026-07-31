import { OverlayContainer } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentRecord } from '../../core/piip.models';
import { DocumentsComponent } from './documents.component';

describe('DocumentsComponent operations', () => {
  let overlayContainer: OverlayContainer;

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
    overlayContainer = TestBed.inject(OverlayContainer);
  });

  afterEach(() => {
    overlayContainer.getContainerElement().innerHTML = '';
  });

  it('renders one accessible gear per actionable document without direct action buttons', () => {
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    const triggers = host.querySelectorAll<HTMLButtonElement>('.document-action-trigger');
    expect(triggers).toHaveLength(6);
    expect(triggers[0].getAttribute('aria-label')).toBe('Acciones de Ficha de Iniciativa de Innovación Pública');
    expect(triggers[0].textContent?.trim()).toBe('settings');
    expect(host.querySelector('.actions .secondary-button, .actions .text-button, .actions .icon-action')).toBeNull();
  });

  it('shows the applicable administrator options for loaded and pending documents', async () => {
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    const loadedTrigger = host.querySelector<HTMLButtonElement>('[aria-label="Acciones de Ficha de Iniciativa de Innovación Pública"]');
    loadedTrigger?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    let menuPanels = overlayContainer.getContainerElement().querySelectorAll<HTMLElement>('.mat-mdc-menu-content');
    let menuText = menuPanels.item(menuPanels.length - 1).textContent ?? '';
    expect(menuText).toContain('Descargar');
    expect(menuText).toContain('Publicar para consulta externa');
    expect(menuText).not.toContain('Marcar como No aplica');

    loadedTrigger?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const pendingTrigger = host.querySelector<HTMLButtonElement>('[aria-label="Acciones de Documento formal de decisión de aprobación"]');
    pendingTrigger?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    menuPanels = overlayContainer.getContainerElement().querySelectorAll<HTMLElement>('.mat-mdc-menu-content');
    menuText = menuPanels.item(menuPanels.length - 1).textContent ?? '';
    expect(menuText).toContain('Marcar como No aplica');
    expect(menuText).not.toContain('Descargar');
  });

  it('limits Consulta externa to download actions', async () => {
    TestBed.inject(PiipMockRepository).role.set('Consulta externa');
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    const triggers = host.querySelectorAll<HTMLButtonElement>('.document-action-trigger');
    expect(triggers).toHaveLength(2);
    triggers[0].click();
    fixture.detectChanges();
    await fixture.whenStable();

    const menuText = overlayContainer.getContainerElement().textContent ?? '';
    expect(menuText).toContain('Descargar');
    expect(menuText).not.toContain('Publicar para consulta externa');
    expect(menuText).not.toContain('Retirar publicación');
    expect(menuText).not.toContain('Marcar como No aplica');
  });

  it('shows Retirar publicación for an externally published document', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.documentDossiers()[0].stages[0].records[0].externallyPublished = true;
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    host.querySelector<HTMLButtonElement>('[aria-label="Acciones de Ficha de Iniciativa de Innovación Pública"]')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const menuText = overlayContainer.getContainerElement().textContent ?? '';
    expect(menuText).toContain('Retirar publicación');
    expect(menuText).not.toContain('Publicar para consulta externa');
  });

  it('invokes download once from the document menu', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.documentDossiers()[0].stages[0].records[0].versionId = 25;
    const downloadDocument = vi.spyOn(repository, 'downloadDocument').mockResolvedValue(undefined);
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    host.querySelector<HTMLButtonElement>('[aria-label="Acciones de Ficha de Iniciativa de Innovación Pública"]')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    overlayContainer.getContainerElement().querySelector<HTMLButtonElement>('[mat-menu-item]')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(downloadDocument).toHaveBeenCalledTimes(1);
  });

  it('shows the active row spinner and disables every gear during an operation', () => {
    const fixture = TestBed.createComponent(DocumentsComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const activeDocument = fixture.componentInstance.dossier()!.stages[0].records[0];

    fixture.componentInstance.pendingOperation.set({ kind: 'download', key: fixture.componentInstance.operationKey(activeDocument) });
    fixture.detectChanges();

    const triggers = Array.from(host.querySelectorAll<HTMLButtonElement>('.document-action-trigger'));
    const activeTrigger = triggers.find((trigger) => trigger.getAttribute('aria-label')?.includes(activeDocument.name));
    expect(triggers.every((trigger) => trigger.disabled)).toBe(true);
    expect(activeTrigger?.getAttribute('aria-busy')).toBe('true');
    expect(activeTrigger?.textContent?.trim()).toBe('progress_activity');
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
