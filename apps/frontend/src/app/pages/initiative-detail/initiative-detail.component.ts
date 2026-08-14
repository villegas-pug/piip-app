import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DocumentRecord, PiipStatus } from '../../core/piip.models';
import { presentAuditEvent } from '../audit/audit-event.presenter';

const TECHNICAL_REPORT = 'Informe de opinión técnica de evaluación de iniciativa';
const FORMAL_DECISION = 'Documento formal de decisión de aprobación';

@Component({
  selector: 'app-initiative-detail',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './initiative-detail.component.html',
  styleUrl: './initiative-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly paramMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });

  readonly code = computed(() => this.paramMap().get('code') ?? '');
  readonly detail = computed(() => this.repository.getInitiativeDetail(this.code()));
  readonly canAdministerRecord = computed(() => this.repository.canAdministerExecutingUnit(this.detail()?.initiative.executingUnitId));
  readonly decisionOpen = signal(this.route.snapshot.queryParamMap.get('action') === 'approve');
  readonly approvalComplete = signal(false);
  readonly submitting = signal(false);
  readonly decisionForm = this.formBuilder.nonNullable.group({ observation: [''] });
  readonly approvalDocuments = computed(() => [
    this.findDocument(TECHNICAL_REPORT),
    this.findDocument(FORMAL_DECISION),
  ].filter((document): document is DocumentRecord => Boolean(document)));
  readonly missingApprovalDocuments = computed(() =>
    this.approvalDocuments().filter((document) => document.state !== 'Cargado'),
  );
  readonly descendingAuditEvents = computed(() =>
    this.repository.auditEvents().filter((event) => event.recordCode === this.code()),
  );
  readonly timeline = computed(() =>
    [...this.descendingAuditEvents()].reverse().map(presentAuditEvent),
  );

  openApproval(): void {
    const detail = this.detail();
    if (!this.canAdministerRecord() || detail?.initiative.status !== 'Presentado') return;
    this.decisionOpen.set(true);
  }

  async approve(): Promise<void> {
    if (this.submitting()) return;
    this.submitting.set(true);
    try {
      await Promise.resolve(this.repository.approveInitiative({
        initiativeCode: this.code(),
        targetStatus: 'Iniciativa aprobada',
        observation: this.decisionForm.controls.observation.value,
      }));
      this.decisionOpen.set(false);
      this.approvalComplete.set(true);
      this.snackBar.open('Iniciativa aprobada. El proyecto aún no ha sido creado.', 'Cerrar', { duration: 3800 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible aprobar la iniciativa.', 'Cerrar', { duration: 4200 });
    } finally {
      this.submitting.set(false);
    }
  }

  statusClass(status: PiipStatus): string {
    if (status === 'Iniciativa aprobada') return 'approved';
    if (status === 'Iniciativa archivada') return 'archived';
    if (status === 'No Admisible' || status === 'No Aplicable') return 'rejected';
    return '';
  }

  private findDocument(name: string): DocumentRecord | undefined {
    return this.detail()?.dossier?.stages.flatMap((stage) => stage.records).find((document) => document.name === name);
  }
}
