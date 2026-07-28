import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { summarizeDocumentDossier } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentRecord, DocumentStage, DocumentType, PiipRecordType, PiipStatus } from '../../core/piip.models';

@Component({
  selector: 'app-documents',
  imports: [MatIconModule, RouterLink],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly routeParamMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  readonly repository = inject(PIIP_REPOSITORY);
  readonly collapsedStages = signal<Set<string>>(new Set());
  readonly uploadOpen = signal(false);
  readonly uploadType = signal<DocumentType>('PUBLIC_INNOVATION_INITIATIVE_SHEET');
  readonly uploadFile = signal<File | null>(null);
  readonly operationPending = signal(false);
  readonly documentTypes: { type: DocumentType; label: string }[] = [
    { type: 'PUBLIC_INNOVATION_INITIATIVE_SHEET', label: 'Ficha de Iniciativa de Innovación Pública' },
    { type: 'INITIATIVE_TECHNICAL_OPINION', label: 'Informe de opinión técnica de evaluación de iniciativa' },
    { type: 'FORMAL_APPROVAL_DECISION', label: 'Documento formal de decisión de aprobación' },
    { type: 'FINAL_PRODUCT_APPROVAL', label: 'Documento formal de aprobación de producto final' },
    { type: 'PROJECT_MANAGEMENT_DOCUMENTATION', label: 'Documentación de la gestión del proyecto' },
    { type: 'FINAL_CLOSURE_REPORT', label: 'Informe final de cierre' },
  ];
  readonly code = computed(() => this.routeParamMap().get('code') ?? '');
  readonly recordType = computed<PiipRecordType>(() => this.routeData()['recordType'] === 'Proyecto' ? 'Proyecto' : 'Iniciativa');
  readonly dossier = computed(() => this.repository.getDocumentDossier(this.recordType(), this.code()));
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
    this.uploadType.set((event.target as HTMLSelectElement).value as DocumentType);
  }

  selectUploadFile(event: Event): void {
    this.uploadFile.set((event.target as HTMLInputElement).files?.[0] ?? null);
  }

  async upload(): Promise<void> {
    const file = this.uploadFile();
    if (!file) return;
    this.operationPending.set(true);
    try {
      await Promise.resolve(this.repository.uploadDocument(this.code(), this.uploadType(), file));
      this.uploadOpen.set(false);
      this.uploadFile.set(null);
      this.snackBar.open('Documento cargado correctamente.', 'Cerrar', { duration: 3000 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cargar el documento.', 'Cerrar', { duration: 4000 });
    } finally {
      this.operationPending.set(false);
    }
  }

  async download(document: DocumentRecord): Promise<void> {
    if (!document.versionId || !document.filename) return;
    try {
      await Promise.resolve(this.repository.downloadDocument(this.code(), document.versionId, document.filename));
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible descargar el documento.', 'Cerrar', { duration: 4000 });
    }
  }

  async togglePublication(document: DocumentRecord): Promise<void> {
    if (!document.versionId || document.optimisticVersion === undefined) return;
    try {
      await Promise.resolve(this.repository.setDocumentPublication(this.code(), document.versionId, !document.externallyPublished, document.optimisticVersion));
      this.snackBar.open(document.externallyPublished ? 'Documento retirado de consulta externa.' : 'Documento publicado para consulta externa.', 'Cerrar', { duration: 3200 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cambiar la publicación.', 'Cerrar', { duration: 4000 });
    }
  }

  async markNotApplicable(document: DocumentRecord): Promise<void> {
    if (!document.type) return;
    try {
      await Promise.resolve(this.repository.markDocumentNotApplicable(this.code(), document.type, 'Marcado desde el expediente PIIP'));
      this.snackBar.open('Documento marcado como No aplica.', 'Cerrar', { duration: 3000 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible actualizar el documento.', 'Cerrar', { duration: 4000 });
    }
  }
}
