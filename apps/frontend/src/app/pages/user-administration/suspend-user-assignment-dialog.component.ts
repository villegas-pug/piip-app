import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import type { ScopeResponse } from '../../api/generated';

export interface SuspendUserAssignmentDialogData {
  scope: ScopeResponse;
  userName: string;
  userEmail: string;
}

@Component({
  selector: 'app-suspend-user-assignment-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './suspend-user-assignment-dialog.component.html',
  styleUrl: './suspend-user-assignment-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuspendUserAssignmentDialogComponent {
  readonly data = inject<SuspendUserAssignmentDialogData>(MAT_DIALOG_DATA);

  roleLabel(): string {
    return this.data.scope.role === 'ADMINISTRADOR_PIIP' ? 'Administrador PIIP' : 'Consulta externa';
  }

  executingUnitLabel(): string {
    return this.data.scope.executingUnit ?? 'Toda la institución';
  }
}
