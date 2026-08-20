import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { INITIATIVE_STATUS_TRANSITIONS, type InitiativeStatus } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import type { DocumentRecord, InitiativeDetail, PiipStatus } from '../../core/piip.models';
import { presentAuditEvent, type PresentedAuditEvent } from '../audit/audit-event.presenter';
import { initiativeStatusVisual, type InitiativeStatusVisual } from '../initiatives/initiative-status-visual';
import { projectStatusVisual, type ProjectStatusVisual } from '../projects/project-status-visual';
import { InitiativeApprovalDialogComponent, type InitiativeApprovalDialogResult } from './initiative-approval-dialog.component';
import { InitiativeStatusTransitionDialogComponent, type InitiativeStatusTransitionDialogResult } from './initiative-status-transition-dialog.component';

const TECHNICAL_REPORT = 'Informe de opinión técnica de evaluación de iniciativa';
const FORMAL_DECISION = 'Documento formal de decisión de aprobación';

@Component({
  selector: 'app-initiative-detail',
  imports: [RouterLink, MatIconModule],
  templateUrl: './initiative-detail.component.html',
  styleUrl: './initiative-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly paramMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly approvalRequested = this.route.snapshot.queryParamMap.get('action') === 'approve';
  private approvalRequestHandled = false;

  readonly code = computed(() => this.paramMap().get('code') ?? '');
  readonly detail = computed(() => this.repository.getInitiativeDetail(this.code()));
  readonly canAdministerRecord = computed(() => this.repository.canAdministerExecutingUnit(this.detail()?.initiative.executingUnitId));
  readonly initiativeTransitionOptions = computed(() => {
    const current = this.detail()?.initiative.status as InitiativeStatus | undefined;
    if (!current || this.detail()?.derivedProject) return [] as readonly InitiativeStatus[];
    return INITIATIVE_STATUS_TRANSITIONS[current] ?? [];
  });
  readonly dossierDocuments = computed(() => this.detail()?.dossier?.stages.flatMap((stage) => stage.records) ?? []);
  readonly approvalDocuments = computed(() => [
    this.findDocument(TECHNICAL_REPORT),
    this.findDocument(FORMAL_DECISION),
  ].filter((document): document is DocumentRecord => Boolean(document)));
  readonly timeline = computed(() => this.repository.auditEvents()
    .filter((event) => event.recordCode === this.code())
    .map(presentAuditEvent));
  readonly recentTimeline = computed(() => this.timeline().slice(0, 3));
  readonly executingUnit = computed(() => {
    const executingUnitId = this.detail()?.portfolioRecord.executingUnitId ?? this.detail()?.initiative.executingUnitId;
    return this.repository.executingUnits().find((unit) => unit.id === executingUnitId);
  });
  readonly responsibleUnitsLabel = computed(() => {
    const record = this.detail()?.portfolioRecord;
    const references = record?.responsibleUnitReferences ?? [];
    if (references.length) {
      return references.map((unit) => unit.acronym ? `${unit.acronym} — ${unit.name}` : unit.name).join(', ');
    }
    return record?.responsibleUnits || 'Sin información registrada';
  });

  constructor() {
    effect(() => {
      const detail = this.detail();
      if (!this.approvalRequested || this.approvalRequestHandled || !detail) return;
      this.approvalRequestHandled = true;
      this.openApproval();
    });
  }

  openApproval(): void {
    const detail = this.detail();
    if (!detail || !this.canAdministerRecord() || detail.initiative.status !== 'Presentado' || detail.derivedProject) return;

    this.dialog.open(InitiativeApprovalDialogComponent, {
      width: '600px',
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      autoFocus: 'first-heading',
      restoreFocus: true,
      closeOnNavigation: true,
      panelClass: 'initiative-review-dialog-panel',
      backdropClass: 'initiative-review-dialog-backdrop',
      data: {
        initiativeCode: detail.initiative.code,
        initiativeName: detail.initiative.name,
        currentStatus: detail.initiative.status,
        approvalDocuments: this.approvalDocuments(),
      },
    }).afterClosed().subscribe((result: InitiativeApprovalDialogResult | undefined) => {
      if (result?.approved) this.snackBar.open('Iniciativa aprobada. El proyecto aún no ha sido creado.', 'Cerrar', { duration: 3800 });
    });
  }

  openStatusDialog(): void {
    const detail = this.detail();
    if (!detail || !this.canAdministerRecord() || detail.derivedProject || !this.initiativeTransitionOptions().length) return;

    this.dialog.open(InitiativeStatusTransitionDialogComponent, {
      width: '560px',
      maxWidth: 'calc(100vw - 24px)',
      maxHeight: '90dvh',
      autoFocus: 'first-heading',
      restoreFocus: true,
      closeOnNavigation: true,
      panelClass: 'initiative-review-dialog-panel',
      backdropClass: 'initiative-review-dialog-backdrop',
      data: {
        initiativeCode: detail.initiative.code,
        currentStatus: detail.initiative.status as InitiativeStatus,
        options: this.initiativeTransitionOptions(),
      },
    }).afterClosed().subscribe((result: InitiativeStatusTransitionDialogResult | undefined) => {
      if (result) this.snackBar.open(`Iniciativa actualizada a ${result.targetStatus}.`, 'Cerrar', { duration: 3800 });
    });
  }

  statusVisual(status: PiipStatus | string): InitiativeStatusVisual {
    return initiativeStatusVisual(status);
  }

  derivedProjectStatusVisual(status: PiipStatus | string): ProjectStatusVisual {
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

  activityDocumentName(event: PresentedAuditEvent, detail: InitiativeDetail): string | null {
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

  private findDocument(name: string): DocumentRecord | undefined {
    return this.detail()?.dossier?.stages.flatMap((stage) => stage.records).find((document) => document.name === name);
  }
}
