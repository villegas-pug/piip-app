import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { summarizeDocumentDossier } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentFile, DocumentRecord, DocumentStage, DocumentVersion, PiipRecordType, PiipStatus } from '../../core/piip.models';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';
import { DeleteDocumentFileDialogComponent } from './delete-document-file-dialog.component';
import { presentDocumentTypeLabel } from './document-type-label.presenter';

type DocumentOperationKind = 'add-file' | 'new-version' | 'delete-file' | 'download' | 'publication' | 'not-applicable';

interface PendingDocumentOperation {
  kind: DocumentOperationKind;
  key: string;
}

@Component({
  selector: 'app-documents',
  imports: [MatIconModule, MatMenuModule, RouterLink, PiipPaginationComponent],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly routeParamMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  readonly repository = inject(PIIP_REPOSITORY);
  readonly collapsedStages = signal<Set<string>>(new Set());
  readonly expandedFileHistories = signal<Set<string>>(new Set());
  readonly uploadOpen = signal(false);
  readonly uploadType = signal<number | null>(null);
  readonly uploadFile = signal<File | null>(null);
  readonly uploadFileError = signal<string | null>(null);
  readonly uploadTarget = signal<{ document: DocumentRecord; file: DocumentFile } | null>(null);
  readonly pendingOperation = signal<PendingDocumentOperation | null>(null);
  readonly stagePageIndexes = signal<Record<string, number>>({});
  readonly operationPending = computed(() => this.pendingOperation() !== null);
  readonly catalogState = this.repository.catalogs;
  readonly documentTypes = computed(() => this.catalogState().value.documentTypes);
  readonly code = computed(() => this.routeParamMap().get('code') ?? '');
  readonly recordType = computed<PiipRecordType>(() => this.routeData()['recordType'] === 'Proyecto' ? 'Proyecto' : 'Iniciativa');
  readonly dossier = computed(() => this.repository.getDocumentDossier(this.recordType(), this.code()));
  readonly canAdministerRecord = computed(() => this.repository.canAdministerExecutingUnit(this.dossier()?.executingUnitId));
  readonly summary = computed(() => {
    const dossier = this.dossier();
    return dossier ? summarizeDocumentDossier(dossier) : undefined;
  });
  readonly moduleRoute = computed(() => this.recordType() === 'Iniciativa' ? '/iniciativas' : '/proyectos');
  readonly moduleLabel = computed(() => this.recordType() === 'Iniciativa' ? 'Iniciativas' : 'Proyectos');
  readonly project = computed(() => this.recordType() === 'Proyecto'
    ? this.repository.projects().find((project) => project.code === this.code())
    : undefined,
  );
  readonly progress = computed(() => {
    const summary = this.summary();
    if (!summary) return 0;
    const applicableDocuments = summary.loadedCount + summary.pendingCount;
    return applicableDocuments ? Math.round((summary.loadedCount / applicableDocuments) * 100) : 0;
  });

  toggleStage(title: string): void {
    this.collapsedStages.update((current) => {
      const next = new Set(current);
      next.has(title) ? next.delete(title) : next.add(title);
      return next;
    });
  }

  stageAvailability(stage: DocumentStage): string {
    const loaded = stage.records.filter((document) => document.state === 'Cargado').length;
    const pending = stage.records.filter((document) => document.state === 'Pendiente').length;
    if (!loaded && !pending) return 'No aplica';
    return `${loaded} cargado${loaded === 1 ? '' : 's'} · ${pending} pendiente${pending === 1 ? '' : 's'}`;
  }

  statusClass(status: PiipStatus): string {
    if (status === 'Iniciativa aprobada') return 'approved';
    if (status === 'Proyecto en ejecución') return 'running';
    if (status === 'Producto aprobado') return 'product';
    if (status === 'Suspendido') return 'suspended';
    if (status === 'Finalizado') return 'finalized';
    if (status === 'Cancelado' || status === 'Iniciativa archivada') return 'archived';
    if (status === 'No Admisible' || status === 'No Aplicable' || status === 'Producto no aprobado') return 'rejected';
    return '';
  }

  selectUploadType(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.uploadType.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  stagePageIndex(stage: DocumentStage): number {
    return clampPageIndex(this.stagePageIndexes()[stage.title] ?? 0, stage.records.length);
  }

  pagedStageRecords(stage: DocumentStage): readonly DocumentRecord[] {
    return paginateItems(stage.records, this.stagePageIndex(stage));
  }

  shouldShowStagePagination(stage: DocumentStage): boolean {
    return stage.records.length > 5;
  }

  setStagePage(stage: DocumentStage, pageIndex: number): void {
    this.stagePageIndexes.update((current) => ({ ...current, [stage.title]: clampPageIndex(pageIndex, stage.records.length) }));
  }

  selectUploadFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.uploadFile.set(file);
    this.uploadFileError.set(file && !this.hasSupportedExtension(file)
      ? `El formato de ${this.fileExtension(file) || 'archivo'} no está permitido. Selecciona un archivo PDF, DOCX o XLSX.`
      : null);
  }

  toggleUploadPanel(): void {
    this.openAddFilePanel();
  }

  openAddFilePanel(): void {
    if (!this.canAdministerRecord()) return;
    if (this.uploadOpen()) {
      this.closeUploadPanel();
      return;
    }
    this.uploadTarget.set(null);
    this.uploadType.set(null);
    this.uploadFile.set(null);
    this.uploadFileError.set(null);
    this.uploadOpen.set(true);
  }

  openNewVersionPanel(document: DocumentRecord, file: DocumentFile): void {
    if (!this.canAdministerRecord() || file.id === null || this.operationPending()) return;
    this.uploadTarget.set({ document, file });
    this.uploadType.set(document.documentTypeId ?? null);
    this.uploadFile.set(null);
    this.uploadFileError.set(null);
    this.uploadOpen.set(true);
  }

  closeUploadPanel(): void {
    this.uploadOpen.set(false);
    this.uploadFile.set(null);
    this.uploadFileError.set(null);
    this.uploadTarget.set(null);
    this.uploadType.set(null);
  }

  async upload(): Promise<void> {
    const file = this.uploadFile();
    const documentTypeId = this.uploadType();
    const target = this.uploadTarget();
    if (!file || this.uploadFileError() || !documentTypeId || (target && target.file.id === null) || this.operationPending() || !this.canAdministerRecord()) return;
    this.pendingOperation.set({ kind: target ? 'new-version' : 'add-file', key: target ? this.fileKey(target.file) : String(documentTypeId) });
    try {
      if (target?.file.id !== undefined && target.file.id !== null) {
        await Promise.resolve(this.repository.addDocumentFileVersion(this.code(), target.file.id, file));
      } else {
        await Promise.resolve(this.repository.addDocumentFile(this.code(), documentTypeId, file));
      }
      this.closeUploadPanel();
      this.snackBar.open(target ? 'Nueva versión cargada correctamente.' : 'Archivo agregado correctamente.', 'Cerrar', { duration: 3000 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cargar el documento.', 'Cerrar', { duration: 4000 });
    } finally {
      this.pendingOperation.set(null);
    }
  }

  async download(version: DocumentVersion): Promise<void> {
    if (!version.id || this.operationPending()) return;
    this.pendingOperation.set({ kind: 'download', key: this.versionKey(version) });
    try {
      await Promise.resolve(this.repository.downloadDocument(this.code(), version.id, version.filename));
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible descargar el documento.', 'Cerrar', { duration: 4000 });
    } finally {
      this.pendingOperation.set(null);
    }
  }

  async togglePublication(version: DocumentVersion): Promise<void> {
    if (!version.id || version.optimisticVersion === undefined || this.operationPending() || !this.canAdministerRecord()) return;
    this.pendingOperation.set({ kind: 'publication', key: this.versionKey(version) });
    try {
      await Promise.resolve(this.repository.setDocumentPublication(this.code(), version.id, !version.externallyPublished, version.optimisticVersion));
      this.snackBar.open(version.externallyPublished ? 'Documento retirado de consulta externa.' : 'Documento publicado para consulta externa.', 'Cerrar', { duration: 3200 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cambiar la publicación.', 'Cerrar', { duration: 4000 });
    } finally {
      this.pendingOperation.set(null);
    }
  }

  async requestDeleteFile(document: DocumentRecord, file: DocumentFile): Promise<void> {
    if (file.id === null || this.operationPending() || !this.canAdministerRecord()) return;
    const confirmed = await firstValueFrom(this.dialog.open(DeleteDocumentFileDialogComponent, {
      data: {
        documentName: document.name,
        filename: file.current?.filename ?? `Archivo ${file.id}`,
        latestVersion: file.latestVersion,
      },
      autoFocus: 'first-tabbable',
      maxWidth: 'calc(100vw - 32px)',
    }).afterClosed());
    if (confirmed !== true) return;
    await this.deleteFile(file);
  }

  async deleteFile(file: DocumentFile): Promise<void> {
    if (file.id === null || this.operationPending() || !this.canAdministerRecord()) return;
    this.pendingOperation.set({ kind: 'delete-file', key: this.fileKey(file) });
    try {
      await Promise.resolve(this.repository.deleteDocumentFile(this.code(), file.id));
      this.snackBar.open('Archivo eliminado correctamente.', 'Cerrar', { duration: 3000 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible eliminar el archivo.', 'Cerrar', { duration: 4000 });
    } finally {
      this.pendingOperation.set(null);
    }
  }

  async markNotApplicable(document: DocumentRecord): Promise<void> {
    if (!document.documentTypeId || this.operationPending() || !this.canAdministerRecord()) return;
    this.pendingOperation.set({ kind: 'not-applicable', key: this.documentKey(document) });
    try {
      await Promise.resolve(this.repository.markDocumentNotApplicable(this.code(), document.documentTypeId, 'Marcado desde el expediente PIIP'));
      this.snackBar.open('Documento marcado como No aplica.', 'Cerrar', { duration: 3000 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible actualizar el documento.', 'Cerrar', { duration: 4000 });
    } finally {
      this.pendingOperation.set(null);
    }
  }

  isPending(kind: DocumentOperationKind, key?: string): boolean {
    const operation = this.pendingOperation();
    return operation?.kind === kind && (!key || operation.key === key);
  }

  isDocumentPending(document: DocumentRecord): boolean {
    return this.pendingOperation()?.key === this.documentKey(document);
  }

  isHistoryExpanded(file: DocumentFile): boolean {
    return this.expandedFileHistories().has(this.fileKey(file));
  }

  toggleFileHistory(file: DocumentFile): void {
    this.expandedFileHistories.update((current) => {
      const next = new Set(current);
      const key = this.fileKey(file);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  historyVersions(file: DocumentFile): readonly DocumentVersion[] {
    const current = file.current;
    if (!current) return file.versions;
    return file.versions.filter((version) => current.id !== undefined && version.id !== undefined
      ? version.id !== current.id
      : version.number !== current.number);
  }

  historyId(file: DocumentFile): string {
    return `document-history-${file.id ?? file.current?.id ?? 'legacy'}`;
  }

  documentTypeLabel(name: string | null | undefined, code?: string | null): string {
    return presentDocumentTypeLabel(name, code);
  }

  documentKey(document: DocumentRecord): string {
    return String(document.documentTypeId ?? document.type ?? document.name);
  }

  fileKey(file: DocumentFile): string {
    return `file:${file.id ?? file.current?.id ?? 'legacy'}`;
  }

  versionKey(version: DocumentVersion): string {
    return `version:${version.id ?? version.number}`;
  }

  private hasSupportedExtension(file: File): boolean {
    return ['.pdf', '.docx', '.xlsx'].includes(this.fileExtension(file));
  }

  private fileExtension(file: File): string {
    const dot = file.name.lastIndexOf('.');
    return dot < 0 ? '' : file.name.slice(dot).toLocaleLowerCase();
  }
}
