import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import type { AdministrativeExecutingUnit } from '../../core/piip.models';

export interface ExecutingUnitStateDialogData { unit: AdministrativeExecutingUnit; }

@Component({
  selector: 'app-executing-unit-state-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './executing-unit-state-dialog.component.html',
  styleUrl: './executing-unit-state-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutingUnitStateDialogComponent {
  readonly data = inject<ExecutingUnitStateDialogData>(MAT_DIALOG_DATA);
  readonly action = this.data.unit.active ? 'desactivar' : 'reactivar';
}
