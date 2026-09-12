import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import type { AdministrativeOrganizationalUnit } from '../../core/piip.models';

export interface OrganizationalUnitStateDialogData { unit: AdministrativeOrganizationalUnit; }

@Component({
  selector: 'app-organizational-unit-state-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './organizational-unit-state-dialog.component.html',
  styleUrl: './organizational-unit-state-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationalUnitStateDialogComponent {
  readonly data = inject<OrganizationalUnitStateDialogData>(MAT_DIALOG_DATA);
}
