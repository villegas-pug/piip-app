import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { PIIP_CATALOGS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentDossierSummary, PiipStatus } from '../../core/piip.models';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';

type StatusTone = 'pending' | 'success' | 'progress' | 'neutral' | 'warning' | 'danger';

interface StatusVisual {
  readonly icon: string;
  readonly tone: StatusTone;
}

const STATUS_VISUALS: Readonly<Record<string, StatusVisual>> = {
  Presentado: { icon: 'schedule', tone: 'pending' },
  'Iniciativa aprobada': { icon: 'check_circle', tone: 'success' },
  'Producto aprobado': { icon: 'check_circle', tone: 'success' },
  Finalizado: { icon: 'check_circle', tone: 'success' },
  'Proyecto en ejecución': { icon: 'play_circle', tone: 'progress' },
  'Iniciativa archivada': { icon: 'archive', tone: 'neutral' },
  'No Aplicable': { icon: 'remove_circle_outline', tone: 'neutral' },
  Suspendido: { icon: 'pause_circle', tone: 'warning' },
  'Producto no aprobado': { icon: 'cancel', tone: 'danger' },
  'No Admisible': { icon: 'cancel', tone: 'danger' },
  Cancelado: { icon: 'cancel', tone: 'danger' },
};

const FALLBACK_STATUS_VISUAL: StatusVisual = { icon: 'circle', tone: 'neutral' };

@Component({
  selector: 'app-documents-inbox',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule, PiipPaginationComponent],
  templateUrl: './documents-inbox.component.html',
  styleUrl: './documents-inbox.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsInboxComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly recordTypes = computed(() => this.repository.catalogs().value.recordTypes);
  readonly units = this.repository.organizationalUnits;
  readonly catalogState = this.repository.catalogs;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', recordType: 'Todos', status: 'Todos', unit: 'Todas' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly pageIndex = signal(0);

  readonly summaries = computed(() => this.repository.getDocumentDossierSummaries());
  readonly filteredDossiers = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').trim().toLocaleLowerCase();
    return this.summaries().filter((dossier) =>
      (!search || `${dossier.code} ${dossier.name}`.toLocaleLowerCase().includes(search)) &&
      (value.recordType === 'Todos' || dossier.recordType === value.recordType) &&
      (value.status === 'Todos' || dossier.status === value.status) &&
      (value.unit === 'Todas' || dossier.organizationalUnits?.some((unit) => unit.id === Number(value.unit))),
    );
  });
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.filteredDossiers().length));
  readonly pagedDossiers = computed(() => paginateItems(this.filteredDossiers(), this.currentPage()));
  readonly loadedDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.loadedCount, 0));
  readonly pendingDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.pendingCount, 0));
  readonly notApplicableDocuments = computed(() => this.summaries().reduce((total, dossier) => total + dossier.notApplicableCount, 0));

  constructor() {
    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.pageIndex.set(0));
  }

  resetFilters(): void {
    this.filters.reset({ search: '', recordType: 'Todos', status: 'Todos', unit: 'Todas' });
  }

  dossierRoute(dossier: DocumentDossierSummary): string[] {
    const segment = dossier.recordType === 'Iniciativa' ? 'iniciativas' : 'proyectos';
    return ['/', segment, dossier.code, 'documentos'];
  }

  statusVisual(status: PiipStatus): StatusVisual { return STATUS_VISUALS[status] ?? FALLBACK_STATUS_VISUAL; }
}
