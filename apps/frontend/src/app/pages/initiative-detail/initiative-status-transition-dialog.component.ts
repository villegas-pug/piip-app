import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { INITIATIVE_STATUS_TRANSITIONS, type InitiativeStatus } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { initiativeStatusVisual, type InitiativeStatusVisual } from '../initiatives/initiative-status-visual';

type InitiativeTransitionTarget = Extract<InitiativeStatus, 'Iniciativa archivada' | 'No Admisible'>;

export interface InitiativeStatusTransitionDialogData {
  readonly initiativeCode: string;
  readonly currentStatus: InitiativeStatus;
  readonly options: readonly InitiativeTransitionTarget[];
}

export interface InitiativeStatusTransitionDialogResult {
  readonly targetStatus: InitiativeTransitionTarget;
}

@Component({
  selector: 'app-initiative-status-transition-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule],
  templateUrl: './initiative-status-transition-dialog.component.html',
  styleUrl: './initiative-status-transition-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeStatusTransitionDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<InitiativeStatusTransitionDialogComponent, InitiativeStatusTransitionDialogResult>);
  private readonly repository = inject(PIIP_REPOSITORY);
  readonly data = inject<InitiativeStatusTransitionDialogData>(MAT_DIALOG_DATA);
  readonly options = this.data.options.filter((target) =>
    (INITIATIVE_STATUS_TRANSITIONS[this.data.currentStatus] as readonly InitiativeTransitionTarget[]).includes(target));
  readonly selectedTarget = signal<InitiativeTransitionTarget | null>(null);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly transitionForm = this.formBuilder.nonNullable.group({ observation: [''] });
  private readonly observationValue = toSignal(this.transitionForm.controls.observation.valueChanges, { initialValue: '' });
  readonly observationLength = computed(() => this.observationValue().length);

  statusVisual(status: InitiativeStatus | string): InitiativeStatusVisual {
    return initiativeStatusVisual(status);
  }

  selectTarget(target: InitiativeTransitionTarget): void {
    if (!this.submitting() && this.options.includes(target)) {
      this.selectedTarget.set(target);
      this.error.set(null);
    }
  }

  close(): void {
    if (!this.submitting()) this.dialogRef.close();
  }

  async confirm(): Promise<void> {
    const targetStatus = this.selectedTarget();
    if (!targetStatus || this.submitting() || !this.options.includes(targetStatus)) return;

    this.submitting.set(true);
    this.error.set(null);
    this.dialogRef.disableClose = true;
    try {
      await Promise.resolve(this.repository.transitionInitiativeStatus({
        initiativeCode: this.data.initiativeCode,
        targetStatus,
        observation: this.transitionForm.controls.observation.value,
      }));
      this.dialogRef.close({ targetStatus });
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No fue posible cambiar el estado de la iniciativa.');
    } finally {
      this.dialogRef.disableClose = false;
      this.submitting.set(false);
    }
  }
}
