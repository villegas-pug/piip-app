import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { PIIP_CATALOGS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { responsibleUnitRowErrors } from '../../core/piip-http.repository';
import { InitiativeReviewDialogComponent } from './initiative-review-dialog.component';
import { OrganizationalUnit, ResourcePhase } from '../../core/piip.models';
import { OrganizationalUnitListComponent, OrganizationalUnitListValue } from '../../shared/organizational-unit-list/organizational-unit-list.component';

@Component({
  selector: 'app-initiative-form',
  imports: [ReactiveFormsModule, MatIconModule, OrganizationalUnitListComponent],
  templateUrl: './initiative-form.component.html',
  styleUrl: './initiative-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativeFormComponent {
  readonly pendingCode = 'Se asignará al registrar';
  private readonly formBuilder = inject(FormBuilder);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly dialog = inject(MatDialog);
  private readonly overlay = inject(Overlay);

  readonly catalogs = PIIP_CATALOGS;
  readonly catalogState = this.repository.catalogs;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly units = this.repository.organizationalUnits;
  readonly uploadedFilename = signal<string | null>(null);
  readonly uploadedFile = signal<File | null>(null);
  readonly submitting = signal(false);
  readonly responsibleUnitIds = signal<readonly number[]>([]);
  readonly responsibleUnitRows = signal(1);
  readonly pendingResponsibleUnit = signal(true);
  readonly responsibleUnitErrors = signal<Readonly<Record<number, string>>>({});
  readonly canAdministerActiveScope = computed(() => this.repository.canAdministerExecutingUnit(this.repository.selectedExecutingUnitId()));

  readonly form = this.formBuilder.nonNullable.group({
    recordType: ['Iniciativa', Validators.required],
    code: [{ value: this.pendingCode, disabled: true }],
    originCode: [{ value: 'NA', disabled: true }],
    startDate: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(180)]],
    status: [{ value: 'Presentado', disabled: true }],
    solutionType: [{ value: '', disabled: this.catalogState().phase !== 'ready' }, Validators.required],
    source: [{ value: '', disabled: this.catalogState().phase !== 'ready' }, Validators.required],
    digitalComponent: ['', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    responsible: ['', Validators.required],
    responsibleUnits: [''],
    note: [''],
    peiObjective: [{ value: '', disabled: this.catalogState().phase !== 'ready' }],
    poiActivity: [{ value: '', disabled: this.catalogState().phase !== 'ready' }],
  });

  constructor() {
    effect(() => {
      const catalogsReady = this.catalogState().phase === 'ready';
      const unitsReady = this.unitsState().phase === 'ready';
      this.syncDisabled(this.form.controls.solutionType, !catalogsReady);
      this.syncDisabled(this.form.controls.source, !catalogsReady);
      this.syncDisabled(this.form.controls.peiObjective, !catalogsReady);
      this.syncDisabled(this.form.controls.poiActivity, !catalogsReady);
      const catalogs = this.catalogState().value;
      this.reconcile(this.form.controls.solutionType, catalogs.solutionTypes);
      this.reconcile(this.form.controls.source, catalogs.sources);
      this.reconcile(this.form.controls.peiObjective, catalogs.peiObjectives);
      this.reconcile(this.form.controls.poiActivity, catalogs.poiActivities);
    });
  }

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
    if (!this.dependenciesReady() || this.form.invalid || !this.uploadedFilename() || !this.hasValidUnits()) {
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
        organizationalUnits: this.selectedUnits(),
        uploadedFilename: this.uploadedFilename(),
        registerInitiative: () => this.registerInitiative(),
      },
    });
  }

  async registerInitiative(): Promise<boolean> {
    if (this.submitting() || !this.canAdministerActiveScope() || !this.dependenciesReady() || !this.hasValidUnits()) return false;
    const value = this.form.getRawValue();
    this.submitting.set(true);
    try {
      const record = await Promise.resolve(this.repository.registerInitiative({
        code: value.code, startDate: value.startDate, name: value.name,
        solutionTypeId: Number(value.solutionType), sourceId: Number(value.source), responsible: value.responsible,
        responsibleUnitIds: this.responsibleUnitIds(),
        peiObjectiveId: value.peiObjective ? Number(value.peiObjective) : undefined,
        poiActivityId: value.poiActivity ? Number(value.poiActivity) : undefined, description: value.description,
        note: value.note, digitalComponent: value.digitalComponent as 'Si' | 'No',
        initialFilename: this.uploadedFilename() ?? '',
        initialFile: this.uploadedFile() ?? undefined,
      }));
      this.snackBar.open(`Iniciativa ${record.code} registrada.`, 'Cerrar', { duration: 3200 });
      await this.router.navigate(['/iniciativas', record.code]);
      return true;
    } catch (error) {
      const rowErrors = responsibleUnitRowErrors(error);
      if (rowErrors) {
        this.responsibleUnitErrors.set(rowErrors);
        return false;
      }
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible registrar la iniciativa.', 'Cerrar', { duration: 4200 });
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  private reconcile(control: { value: string; setErrors(errors: Record<string, boolean> | null): void }, options: Array<{ id: number; active: boolean }>): void {
    if (!control.value) return;
    if (!options.some((option) => option.id === Number(control.value) && option.active)) control.setErrors({ unavailable: true });
    else if ((control as { errors?: Record<string, boolean> | null }).errors?.['unavailable']) control.setErrors(null);
  }

  private dependenciesReady(): boolean {
    const catalogs = this.catalogState();
    const units = this.unitsState();
    return catalogs.phase === 'ready' && catalogs.value.solutionTypes.length > 0 && catalogs.value.sources.length > 0
       && units.phase === 'ready' && units.value.length > 0 && this.hasValidUnits();
  }

  selectableUnits(): readonly OrganizationalUnit[] { return this.units().filter((unit) => unit.active && unit.acronym.trim()); }
  selectedUnits(): readonly OrganizationalUnit[] { return this.responsibleUnitIds().flatMap((id) => this.selectableUnits().filter((unit) => unit.id === id)); }
  onUnitsChange(value: OrganizationalUnitListValue): void { this.responsibleUnitIds.set(value.unitIds); this.responsibleUnitRows.set(value.rowCount); this.pendingResponsibleUnit.set(value.hasPendingSelection); this.responsibleUnitErrors.set({}); }
  private hasValidUnits(): boolean { return this.unitsState().phase === 'ready' && this.responsibleUnitIds().length > 0 && !this.pendingResponsibleUnit() && Object.keys(this.responsibleUnitErrors()).length === 0; }

  private syncDisabled(control: AbstractControl, disabled: boolean): void {
    if (disabled && control.enabled) control.disable({ emitEvent: false });
    else if (!disabled && control.disabled) control.enable({ emitEvent: false });
  }
}
