import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import type { PiipRecordType } from '../../core/piip.models';

export interface PendingChangesDialogData {
  recordType: PiipRecordType;
  code: string;
}

@Component({
  selector: 'app-pending-changes-dialog',
  standalone: true,
  imports: [MatDialogModule, MatIconModule],
  templateUrl: './pending-changes-dialog.component.html',
  styleUrl: './pending-changes-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PendingChangesDialogComponent {
  readonly data = inject<PendingChangesDialogData>(MAT_DIALOG_DATA);

  recordLabel(): string {
    return this.data.recordType === 'Iniciativa' ? 'iniciativa' : 'proyecto';
  }
}
