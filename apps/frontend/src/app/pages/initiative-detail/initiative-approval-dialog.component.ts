import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import type { DocumentRecord, PiipStatus } from '../../core/piip.models';
import { initiativeStatusVisual, type InitiativeStatusVisual } from '../initiatives/initiative-status-visual';

export interface InitiativeApprovalDialogData {
  readonly initiativeCode: string;
  readonly initiativeName: string;
  readonly currentStatus: PiipStatus;
  readonly approvalDocuments: readonly DocumentRecord[];
}

export interface InitiativeApprovalDialogResult {
  readonly approved: true;
}

@Component({
  selector: 'app-initiative-approval-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule, RouterLink],
  templateUrl: './initiative-approval-dialog.component.html',
  styleUrl: './initiative-approval-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeApprovalDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<InitiativeApprovalDialogComponent, InitiativeApprovalDialogResult>);
  private readonly repository = inject(PIIP_REPOSITORY);
  readonly data = inject<InitiativeApprovalDialogData>(MAT_DIALOG_DATA);
  readonly submitting = signal(false);
  readonly approved = signal(false);
  readonly error = signal<string | null>(null);
  readonly approvalForm = this.formBuilder.nonNullable.group({ observation: [''] });
  private readonly observationValue = toSignal(this.approvalForm.controls.observation.valueChanges, { initialValue: '' });
  readonly observationLength = computed(() => this.observationValue().length);
  readonly missingDocuments = computed(() => this.data.approvalDocuments.filter((document) => document.state !== 'Cargado'));

  statusVisual(status: PiipStatus | string): InitiativeStatusVisual {
    return initiativeStatusVisual(status);
  }

  close(): void {
    if (!this.submitting()) this.dialogRef.close(this.approved() ? { approved: true } : undefined);
  }

  async approve(): Promise<void> {
    if (this.submitting() || this.approved()) return;

    this.submitting.set(true);
    this.error.set(null);
    this.dialogRef.disableClose = true;
    try {
      await Promise.resolve(this.repository.approveInitiative({
        initiativeCode: this.data.initiativeCode,
        targetStatus: 'Iniciativa aprobada',
        observation: this.approvalForm.controls.observation.value,
      }));
      this.approved.set(true);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No fue posible aprobar la iniciativa.');
    } finally {
      this.dialogRef.disableClose = false;
      this.submitting.set(false);
    }
  }
}
