import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { PIIP_CATALOGS, RESPONSIBLE_UNITS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentDossierSummary, PiipStatus } from '../../core/piip.models';

@Component({
  selector: 'app-documents-inbox',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './documents-inbox.component.html',
  styleUrl: './documents-inbox.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsInboxComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly units = RESPONSIBLE_UNITS;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', recordType: 'Todos', status: 'Todos', unit: 'Todas' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });

  readonly summaries = computed(() => this.repository.getDocumentDossierSummaries());
  readonly filteredDossiers = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').trim().toLocaleLowerCase();
    return this.summaries().filter((dossier) =>
      (!search || `${dossier.code} ${dossier.name}`.toLocaleLowerCase().includes(search)) &&
      (value.recordType === 'Todos' || dossier.recordType === value.recordType) &&
      (value.status === 'Todos' || dossier.status === value.status) &&
      (value.unit === 'Todas' || dossier.unit === value.unit),
    );
  });
  readonly loadedDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.loadedCount, 0));
  readonly pendingDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.pendingCount, 0));
  readonly notApplicableDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.notApplicableCount, 0));

  resetFilters(): void {
    this.filters.reset({ search: '', recordType: 'Todos', status: 'Todos', unit: 'Todas' });
  }

  dossierRoute(dossier: DocumentDossierSummary): string[] {
    const segment = dossier.recordType === 'Iniciativa' ? 'iniciativas' : 'proyectos';
    return ['/', segment, dossier.code, 'documentos'];
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
}
