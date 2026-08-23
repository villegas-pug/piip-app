import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface DerivedProjectReviewDialogData {
  initiativeCode: string;
  projectCode: string;
  name: string;
  startDateIso: string;
  startDate: string;
  solutionType: string;
  source: string;
  digitalComponent: string;
  responsible: string;
  organizationalUnit: string;
  description: string;
  keyResults: string;
  registerProject: () => Promise<boolean>;
}

@Component({
  selector: 'app-derived-project-review-dialog',
  imports: [MatDialogModule, MatIconModule],
  templateUrl: './derived-project-review-dialog.component.html',
  styleUrl: './derived-project-review-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DerivedProjectReviewDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<DerivedProjectReviewDialogComponent>);
  readonly data = inject<DerivedProjectReviewDialogData>(MAT_DIALOG_DATA);
  readonly submitting = signal(false);

  async register(): Promise<void> {
    if (this.submitting()) return;

    this.submitting.set(true);
    this.dialogRef.disableClose = true;
    try {
      if (await this.data.registerProject()) this.dialogRef.close();
    } finally {
      this.dialogRef.disableClose = false;
      this.submitting.set(false);
    }
  }
}
