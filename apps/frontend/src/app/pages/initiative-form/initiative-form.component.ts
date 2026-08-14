import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { PIIP_CATALOGS, RESPONSIBLE_UNITS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeReviewDialogComponent } from './initiative-review-dialog.component';

@Component({
  selector: 'app-initiative-form',
  imports: [ReactiveFormsModule, MatIconModule],
  templateUrl: './initiative-form.component.html',
  styleUrl: './initiative-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeFormComponent {
  readonly pendingCode = 'Se asignará al registrar';
  private readonly formBuilder = inject(FormBuilder);
  private readonly repository = inject(PIIP_REPOSITORY);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly dialog = inject(MatDialog);
  private readonly overlay = inject(Overlay);

  readonly catalogs = PIIP_CATALOGS;
  readonly units = computed(() => this.repository.organizationalUnits().length
    ? this.repository.organizationalUnits().map((unit) => unit.acronym || unit.name)
    : RESPONSIBLE_UNITS);
  readonly uploadedFilename = signal<string | null>(null);
  readonly uploadedFile = signal<File | null>(null);
  readonly submitting = signal(false);
  readonly canAdministerActiveScope = computed(() => this.repository.canAdministerExecutingUnit(this.repository.selectedExecutingUnitId()));

  readonly form = this.formBuilder.nonNullable.group({
    recordType: ['Iniciativa', Validators.required],
    code: [{ value: this.pendingCode, disabled: true }],
    originCode: [{ value: 'NA', disabled: true }],
    startDate: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(180)]],
    status: [{ value: 'Presentado', disabled: true }],
    solutionType: ['', Validators.required],
    source: ['', Validators.required],
    digitalComponent: ['', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    responsible: ['', Validators.required],
    responsibleUnits: ['', Validators.required],
    note: [''],
    peiObjective: [''],
    poiActivity: [''],
  });

  scrollTo(sectionId: string): void {
    this.elementRef.nativeElement.querySelector(`#${sectionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.uploadedFilename.set(file.name);
      this.uploadedFile.set(file);
    }
  }

  saveDraft(): void {
    this.repository.saveDraft({ ...this.form.getRawValue(), uploadedFilename: this.uploadedFilename() });
    this.snackBar.open('Borrador guardado localmente. No es un estado oficial.', 'Cerrar', { duration: 3500 });
  }

  openReview(): void {
    if (!this.canAdministerActiveScope()) {
      this.snackBar.open('No tienes permisos de Administrador PIIP para la Unidad Ejecutora activa.', 'Cerrar', { duration: 4200 });
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.uploadedFilename()) {
      this.snackBar.open('Completa los campos requeridos y adjunta la ficha inicial.', 'Cerrar', { duration: 4200 });
      return;
    }
    this.dialog.open(InitiativeReviewDialogComponent, {
      width: '620px',
      maxWidth: 'calc(100vw - 40px)',
      maxHeight: 'calc(100vh - 40px)',
      autoFocus: 'first-heading',
      restoreFocus: true,
      panelClass: 'initiative-review-dialog-panel',
      backdropClass: 'initiative-review-dialog-backdrop',
      scrollStrategy: this.overlay.scrollStrategies.block(),
      data: {
        pendingCode: this.pendingCode,
        name: this.form.controls.name.value,
        responsible: this.form.controls.responsible.value,
        uploadedFilename: this.uploadedFilename(),
        registerInitiative: () => this.registerInitiative(),
      },
    });
  }

  async registerInitiative(): Promise<boolean> {
    if (this.submitting() || !this.canAdministerActiveScope()) return false;
    const value = this.form.getRawValue();
    this.submitting.set(true);
    try {
      const record = await Promise.resolve(this.repository.registerInitiative({
        code: value.code, startDate: value.startDate, name: value.name,
        solutionType: value.solutionType as 'Solución potencial o adaptable' | 'Solución por definir' | 'No aplica',
        source: value.source, responsible: value.responsible, responsibleUnits: value.responsibleUnits,
        peiObjective: value.peiObjective, poiActivity: value.poiActivity, description: value.description,
        note: value.note, digitalComponent: value.digitalComponent as 'Si' | 'No',
        initialFilename: this.uploadedFilename() ?? '',
        initialFile: this.uploadedFile() ?? undefined,
      }));
      this.snackBar.open(`Iniciativa ${record.code} registrada.`, 'Cerrar', { duration: 3200 });
      await this.router.navigate(['/iniciativas', record.code]);
      return true;
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible registrar la iniciativa.', 'Cerrar', { duration: 4200 });
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
