import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PROJECT_STATUS_TRANSITIONS, type ProjectStatus } from '../../core/piip.catalogs';
import type { PiipStatus, ProjectDetail } from '../../core/piip.models';
import { presentAuditEvent, type PresentedAuditEvent } from '../audit/audit-event.presenter';
import { ProjectStatusTransitionDialogComponent, type ProjectStatusTransitionDialogResult } from './project-status-transition-dialog.component';
import { projectStatusVisual, type ProjectStatusVisual } from '../projects/project-status-visual';

@Component({
  selector: 'app-project-detail',
  imports: [RouterLink, MatIconModule],
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
  readonly detail = computed(() => this.repository.getProjectDetail(this.code()));
  readonly canAdministerRecord = computed(() => this.repository.canAdministerExecutingUnit(this.detail()?.project.executingUnitId));
  readonly transitionOptions = computed(() => {
    const status = this.detail()?.project.status as ProjectStatus | undefined;
    if (!status) return [] as readonly ProjectStatus[];
    return PROJECT_STATUS_TRANSITIONS[status] ?? [];
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
    if (!detail || !this.canAdministerRecord() || !options.length) return;

    this.dialog.open(ProjectStatusTransitionDialogComponent, {
      width: '560px',
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      autoFocus: 'first-heading',
      restoreFocus: true,
      closeOnNavigation: true,
      panelClass: 'project-status-dialog-panel',
      data: { projectCode: detail.project.code, currentStatus: detail.project.status, options },
    }).afterClosed().subscribe((result: ProjectStatusTransitionDialogResult | undefined) => {
      if (result) this.snackBar.open(`Proyecto actualizado a ${result.targetStatus}.`, 'Cerrar', { duration: 3800 });
    });
  }

}
