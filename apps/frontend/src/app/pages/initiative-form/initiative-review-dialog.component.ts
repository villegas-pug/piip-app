import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface InitiativeReviewDialogData {
  pendingCode: string;
  name: string;
  responsible: string;
  uploadedFilename: string | null;
  registerInitiative: () => Promise<boolean>;
}

@Component({
  selector: 'app-initiative-review-dialog',
  imports: [MatDialogModule, MatIconModule],
  templateUrl: './initiative-review-dialog.component.html',
  styleUrl: './initiative-review-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeReviewDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<InitiativeReviewDialogComponent>);
  readonly data = inject<InitiativeReviewDialogData>(MAT_DIALOG_DATA);
  readonly submitting = signal(false);

  async register(): Promise<void> {
    if (this.submitting()) return;

    this.submitting.set(true);
    this.dialogRef.disableClose = true;
    try {
      if (await this.data.registerInitiative()) this.dialogRef.close();
    } finally {
      this.dialogRef.disableClose = false;
      this.submitting.set(false);
    }
  }
}
