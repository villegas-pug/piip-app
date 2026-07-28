import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

export type ProjectRegistrationDialogView = 'type-selection' | 'initiative-selection';

export interface ProjectRegistrationDialogData {
  initialView: ProjectRegistrationDialogView;
}

export type ProjectRegistrationDialogResult =
  | { mode: 'DERIVED_FROM_INITIATIVE'; initiativeCode: string }
  | { mode: 'PREEXISTING' };

@Component({
  selector: 'app-project-registration-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule],
  templateUrl: './project-registration-dialog.component.html',
  styleUrl: './project-registration-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectRegistrationDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ProjectRegistrationDialogComponent, ProjectRegistrationDialogResult>);
  private readonly dialogData = inject<ProjectRegistrationDialogData>(MAT_DIALOG_DATA);
  private readonly repository = inject(PIIP_REPOSITORY);

  readonly view = signal<ProjectRegistrationDialogView>(this.dialogData.initialView);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly selectedInitiativeCode = signal<string | null>(null);
  readonly eligibleInitiatives = computed(() => this.repository.getInitiativesEligibleForProject());
  private readonly searchValue = toSignal(this.searchControl.valueChanges, { initialValue: '' });
  readonly filteredInitiatives = computed(() => {
    const searchTerm = this.searchValue().toLocaleLowerCase().trim();
    if (!searchTerm) return this.eligibleInitiatives();

    return this.eligibleInitiatives().filter((initiative) =>
      `${initiative.code} ${initiative.name} ${initiative.unit} ${initiative.responsible}`
        .toLocaleLowerCase()
        .includes(searchTerm),
    );
  });

  showInitiativeSelection(): void {
    this.view.set('initiative-selection');
  }

  showTypeSelection(): void {
    this.view.set('type-selection');
    this.selectedInitiativeCode.set(null);
    this.searchControl.setValue('');
  }

  selectInitiative(initiativeCode: string): void {
    this.selectedInitiativeCode.set(initiativeCode);
  }

  choosePreexistingProject(): void {
    this.dialogRef.close({ mode: 'PREEXISTING' });
  }

  continueWithInitiative(): void {
    const initiativeCode = this.selectedInitiativeCode();
    const isStillEligible = this.eligibleInitiatives().some((initiative) => initiative.code === initiativeCode);
    if (!initiativeCode || !isStillEligible) return;

    this.dialogRef.close({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode });
  }
}
