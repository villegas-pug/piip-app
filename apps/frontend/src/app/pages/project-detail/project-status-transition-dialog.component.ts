import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import type { ProjectStatus } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import type { PiipStatus } from '../../core/piip.models';
import { projectStatusVisual, type ProjectStatusVisual } from '../projects/project-status-visual';

export interface ProjectStatusTransitionDialogData {
  readonly projectCode: string;
  readonly currentStatus: PiipStatus;
  readonly options: readonly ProjectStatus[];
}

export interface ProjectStatusTransitionDialogResult {
  readonly targetStatus: ProjectStatus;
}

@Component({
  selector: 'app-project-status-transition-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule],
  templateUrl: './project-status-transition-dialog.component.html',
  styleUrl: './project-status-transition-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectStatusTransitionDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProjectStatusTransitionDialogComponent, ProjectStatusTransitionDialogResult>);
  private readonly repository = inject(PIIP_REPOSITORY);
  readonly data = inject<ProjectStatusTransitionDialogData>(MAT_DIALOG_DATA);
  readonly selectedTarget = signal<ProjectStatus | null>(null);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly transitionForm = this.formBuilder.nonNullable.group({ observation: [''] });
  private readonly observationValue = toSignal(this.transitionForm.controls.observation.valueChanges, { initialValue: '' });
  readonly observationLength = computed(() => this.observationValue().length);
  statusVisual(status: PiipStatus | string): ProjectStatusVisual {
    return projectStatusVisual(status);
  }

  selectTarget(target: ProjectStatus): void {
    if (!this.submitting() && this.data.options.includes(target)) {
      this.selectedTarget.set(target);
      this.error.set(null);
    }
  }

  close(): void {
    if (!this.submitting()) this.dialogRef.close();
  }

  async confirm(): Promise<void> {
    const targetStatus = this.selectedTarget();
    if (!targetStatus || this.submitting()) return;

    this.submitting.set(true);
    this.error.set(null);
    this.dialogRef.disableClose = true;
    try {
      await Promise.resolve(this.repository.transitionProjectStatus({
        projectCode: this.data.projectCode,
        targetStatus,
        observation: this.transitionForm.controls.observation.value,
      }));
      this.dialogRef.close({ targetStatus });
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No fue posible cambiar el estado del proyecto.');
    } finally {
      this.dialogRef.disableClose = false;
      this.submitting.set(false);
    }
  }
}
