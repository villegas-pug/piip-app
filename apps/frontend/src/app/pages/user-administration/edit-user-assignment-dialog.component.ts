import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import type { ScopeResponse } from '../../api/generated';
import type { AdministrableScope } from '../../core/piip.models';

type Role = NonNullable<ScopeResponse['role']>;

export interface EditUserAssignmentDialogData {
  scope: ScopeResponse;
  userName: string;
  userEmail: string;
  administrableScopes: AdministrableScope[];
}

export interface EditUserAssignmentDialogResult {
  role: Role;
  institutionId: number;
  executingUnitId: number;
}

@Component({
  selector: 'app-edit-user-assignment-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatSelectModule],
  templateUrl: './edit-user-assignment-dialog.component.html',
  styleUrl: './edit-user-assignment-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditUserAssignmentDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<EditUserAssignmentDialogComponent, EditUserAssignmentDialogResult>);
  readonly data = inject<EditUserAssignmentDialogData>(MAT_DIALOG_DATA);
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    role: [(this.data.scope.role ?? 'CONSULTA_EXTERNA') as Role, Validators.required],
    institutionId: [this.data.scope.institutionId ?? 0, Validators.min(1)],
    executingUnitId: [this.data.scope.executingUnitId ?? 0],
  });
  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  readonly selectedScope = computed(() => this.data.administrableScopes
    .find((scope) => scope.institutionId === this.formValue().institutionId));
  readonly executingUnits = computed(() => this.selectedScope()?.executingUnits ?? []);
  readonly canUseInstitutionWide = computed(() => this.selectedScope()?.institutionWideAllowed ?? false);

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.dialogRef.close(this.form.getRawValue());
  }
}
