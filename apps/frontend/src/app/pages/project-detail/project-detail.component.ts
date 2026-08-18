import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PROJECT_STATUS_TRANSITIONS } from '../../core/piip.catalogs';
import { PiipStatus } from '../../core/piip.models';
import { presentAuditEvent } from '../audit/audit-event.presenter';

@Component({
  selector: 'app-project-detail',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly paramMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });

  readonly code = computed(() => this.paramMap().get('code') ?? '');
  readonly detail = computed(() => this.repository.getProjectDetail(this.code()));
  readonly canAdministerRecord = computed(() => this.repository.canAdministerExecutingUnit(this.detail()?.project.executingUnitId));
  readonly transitionTarget = signal<PiipStatus | null>(null);
  readonly submitting = signal(false);
  readonly transitionForm = this.formBuilder.nonNullable.group({ observation: [''] });
  readonly transitionOptions = computed(() => {
    const status = this.detail()?.project.status;
    if (!status) return [] as readonly PiipStatus[];
    return PROJECT_STATUS_TRANSITIONS[status as keyof typeof PROJECT_STATUS_TRANSITIONS] ?? [];
  });
  readonly timeline = computed(() => [...this.repository.auditEvents()]
    .filter((event) => event.recordCode === this.code())
    .reverse()
    .map(presentAuditEvent));

  statusClass(status: PiipStatus): string {
    if (status === 'Proyecto en ejecución') return 'running';
    if (status === 'Producto aprobado') return 'product';
    if (status === 'Producto no aprobado') return 'not-approved';
    if (status === 'Suspendido') return 'suspended';
    if (status === 'Finalizado') return 'finalized';
    if (status === 'Cancelado') return 'cancelled';
    return '';
  }

  openStatusTransition(target: PiipStatus): void {
    if (!this.canAdministerRecord() || !this.transitionOptions().includes(target)) return;
    this.transitionTarget.set(target);
    this.transitionForm.reset({ observation: '' });
  }

  closeStatusTransition(): void {
    if (!this.submitting()) this.transitionTarget.set(null);
  }

  async transitionStatus(): Promise<void> {
    const target = this.transitionTarget();
    if (!target || this.submitting()) return;
    this.submitting.set(true);
    try {
      await Promise.resolve(this.repository.transitionProjectStatus({
        projectCode: this.code(),
        targetStatus: target as Extract<PiipStatus, 'Proyecto en ejecución' | 'Producto aprobado' | 'Producto no aprobado' | 'Suspendido' | 'Cancelado' | 'Finalizado'>,
        observation: this.transitionForm.controls.observation.value,
      }));
      this.transitionTarget.set(null);
      this.snackBar.open(`Proyecto actualizado a ${target}.`, 'Cerrar', { duration: 3800 });
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cambiar el estado del proyecto.', 'Cerrar', { duration: 4200 });
    } finally {
      this.submitting.set(false);
    }
  }
}
