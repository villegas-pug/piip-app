import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PROJECT_STATUS_TRANSITIONS, statusDisplayName, type ProjectStatus } from '../../core/piip.catalogs';
import type { PiipStatus, PortfolioStatusReference, ProjectDetail } from '../../core/piip.models';
import { canEditProject } from '../../core/portfolio-edit-permissions';
import { presentAuditEvent, type PresentedAuditEvent } from '../audit/audit-event.presenter';
import { ProjectStatusTransitionDialogComponent, type ProjectStatusTransitionDialogResult } from './project-status-transition-dialog.component';
import { projectStatusVisual, type ProjectStatusVisual } from '../projects/project-status-visual';
import { OrganizationalUnitSummaryComponent } from '../../shared/organizational-unit-summary/organizational-unit-summary.component';

@Component({
  selector: 'app-project-detail',
  imports: [RouterLink, MatIconModule, OrganizationalUnitSummaryComponent],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly paramMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });

  readonly code = computed(() => this.paramMap().get('code') ?? '');
  readonly catalogState = this.repository.catalogs;
  readonly detail = computed(() => this.repository.getProjectDetail(this.code()));
  readonly canAdministerRecord = computed(() => {
    const detail = this.detail();
    return this.repository.canAdministerExecutingUnit(detail?.project.executingUnitId ?? detail?.portfolioRecord.executingUnitId);
  });
  readonly canEditRecord = computed(() => canEditProject(this.detail(), this.canAdministerRecord()));
  /** El origen histórico puede estar inactivo y, aun así, salir por la matriz vigente. */
  readonly statusActionsReady = computed(() => {
    const status = this.detail()?.project.status;
    return this.catalogState().phase === 'ready' && Boolean(status?.code) && status?.active !== undefined;
  });
  readonly transitionOptions = computed(() => {
    const status = this.detail()?.project.status.code as ProjectStatus | undefined;
    if (this.catalogState().phase !== 'ready' || !status) return [] as readonly ProjectStatus[];
    const matrix = PROJECT_STATUS_TRANSITIONS[status] ?? [];
    const statuses = this.catalogState().value.portfolioStatuses;
    return matrix.filter((code) => {
      const option = statuses.find((item) => item.code === code);
      return option?.active === true && option.applicability === 'PROJECT';
    });
  });
  readonly timeline = computed(() => this.repository.auditEvents()
    .filter((event) => event.recordCode === this.code())
    .map(presentAuditEvent));
  readonly recentTimeline = computed(() => this.timeline().slice(0, 3));
  readonly executingUnit = computed(() => {
    const executingUnitId = this.detail()?.portfolioRecord.executingUnitId ?? this.detail()?.project.executingUnitId;
    return this.repository.executingUnits().find((unit) => unit.id === executingUnitId);
  });

  statusVisual(status: PiipStatus | string): ProjectStatusVisual {
    return projectStatusVisual(status);
  }

  statusName(status: PortfolioStatusReference | undefined): string { return statusDisplayName(status); }

  activityKind(event: PresentedAuditEvent): 'document' | 'transition' | 'record' {
    if (event.source.documentName || /documento|cargad|publicad|retirad/i.test(`${event.eventLabel} ${event.source.event}`)) return 'document';
    if (event.eventLabel.toLocaleLowerCase('es-PE').includes('estado') || event.source.event.toLocaleLowerCase('es-PE').includes('estado')) return 'transition';
    return 'record';
  }

  activityIcon(event: PresentedAuditEvent): string {
    const kind = this.activityKind(event);
    if (kind === 'document') return 'description';
    if (kind === 'transition') return 'swap_horiz';
    return event.source.icon || 'history';
  }

  activityDocumentName(event: PresentedAuditEvent, detail: ProjectDetail): string | null {
    if (event.source.documentName) return event.source.documentName;
    const documentTypeCode = event.detailFields.find((field) => field.label === 'Código de tipo documental')?.value;
    if (!documentTypeCode) return null;
    for (const stage of detail.dossier?.stages ?? []) {
      const document = stage.records.find((record) => record.type === documentTypeCode || record.documentType?.code === documentTypeCode);
      if (document?.filename) return document.filename;
    }
    return null;
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value || 'No registrado';
  }

  openStatusDialog(): void {
    const detail = this.detail();
    const options = this.transitionOptions();
    if (!detail || !this.statusActionsReady() || !this.canAdministerRecord() || !options.length) return;

    this.dialog.open(ProjectStatusTransitionDialogComponent, {
      width: '560px',
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      autoFocus: 'first-heading',
      restoreFocus: true,
      closeOnNavigation: true,
      panelClass: 'project-status-dialog-panel',
      data: { projectCode: detail.project.code, currentStatus: detail.project.status.code as ProjectStatus, options },
    }).afterClosed().subscribe((result: ProjectStatusTransitionDialogResult | undefined) => {
      if (result) this.snackBar.open(`Proyecto actualizado a ${result.targetStatus}.`, 'Cerrar', { duration: 3800 });
    });
  }

}
