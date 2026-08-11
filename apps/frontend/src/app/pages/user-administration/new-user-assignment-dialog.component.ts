import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import type { AdministrableScope } from '../../core/piip.models';

export interface NewAssignmentUser {
  subject: string;
  fullName: string;
  email: string;
  withoutAssignments: boolean;
}

export interface NewUserAssignmentDialogData {
  form: FormGroup;
  users: NewAssignmentUser[];
  administrableScopes: AdministrableScope[];
}

interface AssignmentFormValue {
  institutionId: number;
  executingUnitId: number;
}

@Component({
  selector: 'app-new-user-assignment-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatSelectModule],
  templateUrl: './new-user-assignment-dialog.component.html',
  styleUrl: './new-user-assignment-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewUserAssignmentDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<NewUserAssignmentDialogComponent, boolean>);
  readonly data = inject<NewUserAssignmentDialogData>(MAT_DIALOG_DATA);
  private readonly formValue = toSignal(this.data.form.valueChanges, { initialValue: this.data.form.getRawValue() as AssignmentFormValue });
  readonly selectedScope = computed(() => this.data.administrableScopes
    .find((scope) => scope.institutionId === (this.formValue() as AssignmentFormValue).institutionId));
  readonly executingUnits = computed(() => this.selectedScope()?.executingUnits ?? []);
  readonly canUseInstitutionWide = computed(() => this.selectedScope()?.institutionWideAllowed ?? false);
  readonly isInstitutionWide = computed(() => {
    const value = this.formValue() as AssignmentFormValue;
    const scope = this.selectedScope();
    return value.institutionId > 0 && value.executingUnitId === 0 && scope?.institutionWideAllowed === true && Boolean(scope.institutionName);
  });

  assign(): void {
    this.data.form.markAllAsTouched();
    if (this.data.form.invalid) return;
    this.dialogRef.close(true);
  }
}
