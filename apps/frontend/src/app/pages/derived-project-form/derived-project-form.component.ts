import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PIIP_CATALOGS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DerivedProjectInput } from '../../core/piip.models';
import { DerivedProjectReviewDialogComponent, DerivedProjectReviewDialogData } from './derived-project-review-dialog.component';

@Component({
  selector: 'app-derived-project-form',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './derived-project-form.component.html',
  styleUrls: ['./derived-project-form.component.scss', '../initiative-form/initiative-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DerivedProjectFormComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly dialog = inject(MatDialog);
  private readonly overlay = inject(Overlay);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly catalogState = this.repository.catalogs;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly units = this.repository.organizationalUnits;
  readonly initiativeCode = this.route.snapshot.paramMap.get('initiativeCode') ?? '';
  readonly detail = this.repository.getInitiativeDetail(this.initiativeCode);
  readonly inheritedInactive = [
    this.detail?.portfolioRecord.solutionTypeReference,
    this.detail?.portfolioRecord.sourceReference,
    this.detail?.portfolioRecord.peiObjectiveReference,
    this.detail?.portfolioRecord.poiActivityReference,
  ].filter((item) => item && !item.active);
  readonly existingProject = this.repository.getProjectByOrigin(this.initiativeCode);
  readonly provisionalCode = this.repository.getNextProjectCode(this.initiativeCode);
  readonly submitting = signal(false);
  private reviewDialogRef: MatDialogRef<DerivedProjectReviewDialogComponent> | null = null;

  readonly form = this.formBuilder.nonNullable.group({
    recordType: [{ value: 'Proyecto', disabled: true }],
    code: [{ value: this.provisionalCode, disabled: true }],
    originCode: [{ value: this.initiativeCode, disabled: true }],
    status: [{ value: 'Proyecto en ejecución', disabled: true }],
    startDate: ['', Validators.required],
    name: [this.detail?.portfolioRecord.name ?? '', [Validators.required, Validators.maxLength(180)]],
    solutionType: [{ value: this.activeId(this.detail?.portfolioRecord.solutionTypeReference), disabled: this.catalogState().phase !== 'ready' }, Validators.required],
    source: [{ value: this.activeId(this.detail?.portfolioRecord.sourceReference), disabled: this.catalogState().phase !== 'ready' }, Validators.required],
    responsible: [this.detail?.portfolioRecord.responsible ?? '', Validators.required],
    responsibleUnits: [{ value: this.detail?.portfolioRecord.responsibleUnitReferences?.find((item) => item.active)?.id?.toString() ?? '', disabled: this.unitsState().phase !== 'ready' }, Validators.required],
    peiObjective: [{ value: this.activeId(this.detail?.portfolioRecord.peiObjectiveReference), disabled: this.catalogState().phase !== 'ready' }],
    poiActivity: [{ value: this.activeId(this.detail?.portfolioRecord.poiActivityReference), disabled: this.catalogState().phase !== 'ready' }],
    description: [this.detail?.portfolioRecord.description ?? '', [Validators.required, Validators.maxLength(1000)]],
    keyResults: ['', Validators.maxLength(1000)],
    note: ['', Validators.maxLength(600)],
    digitalComponent: [this.detail?.portfolioRecord.digitalComponent ?? '', Validators.required],
  });

  constructor() {
    effect(() => {
      const catalogsReady = this.catalogState().phase === 'ready';
      const unitsReady = this.unitsState().phase === 'ready';
      this.syncDisabled(this.form.controls.solutionType, !catalogsReady);
      this.syncDisabled(this.form.controls.source, !catalogsReady);
      this.syncDisabled(this.form.controls.peiObjective, !catalogsReady);
      this.syncDisabled(this.form.controls.poiActivity, !catalogsReady);
      this.syncDisabled(this.form.controls.responsibleUnits, !unitsReady);
      const catalogs = this.catalogState().value;
      this.reconcile(this.form.controls.solutionType, catalogs.solutionTypes);
      this.reconcile(this.form.controls.source, catalogs.sources);
      this.reconcile(this.form.controls.peiObjective, catalogs.peiObjectives);
      this.reconcile(this.form.controls.poiActivity, catalogs.poiActivities);
      this.reconcile(this.form.controls.responsibleUnits, this.units());
    });
  }

  scrollTo(sectionId: string): void {
    this.elementRef.nativeElement.querySelector(`#${sectionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  saveDraft(): void {
    this.repository.saveDerivedProjectDraft(this.form.getRawValue());
    this.snackBar.open('Borrador del proyecto guardado localmente.', 'Cerrar', { duration: 3300 });
  }

  openReview(): void {
    if (this.reviewDialogRef) return;

    this.form.markAllAsTouched();
    if (!this.dependenciesReady() || this.form.invalid) {
      this.snackBar.open('Completa los campos requeridos antes de revisar el proyecto.', 'Cerrar', { duration: 4000 });
      return;
    }

    const data = this.buildReviewData();
    if (!data) {
      this.snackBar.open('Una opción seleccionada ya no está disponible. Elige una opción vigente.', 'Cerrar', { duration: 4200 });
      return;
    }

    this.reviewDialogRef = this.dialog.open(DerivedProjectReviewDialogComponent, {
      width: '680px',
      maxWidth: 'calc(100vw - 32px)',
      maxHeight: 'calc(100dvh - 32px)',
      autoFocus: 'first-heading',
      restoreFocus: true,
      disableClose: false,
      role: 'dialog',
      ariaLabelledBy: 'derived-project-review-title',
      ariaDescribedBy: 'derived-project-review-description',
      panelClass: 'derived-project-review-dialog-panel',
      backdropClass: 'initiative-review-dialog-backdrop',
      scrollStrategy: this.overlay.scrollStrategies.block(),
      data,
    });
    this.reviewDialogRef.afterClosed().subscribe(() => this.reviewDialogRef = null);
  }

  async registerProject(): Promise<boolean> {
    if (this.submitting() || !this.dependenciesReady()) return false;
    this.submitting.set(true);
    try {
      const record = await Promise.resolve(this.repository.registerDerivedProject(this.buildInput()));
      this.snackBar.open('Proyecto derivado registrado y vinculado con su iniciativa.', 'Cerrar', { duration: 3800 });
      await this.router.navigate(['/proyectos', record.code, 'documentos']);
      return true;
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible registrar el proyecto.', 'Cerrar', { duration: 4300 });
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  private buildReviewData(): DerivedProjectReviewDialogData | null {
    const value = this.form.getRawValue();
    const catalogs = this.catalogState().value;
    const solutionType = catalogs.solutionTypes.find((option) => option.id === Number(value.solutionType) && option.active);
    const source = catalogs.sources.find((option) => option.id === Number(value.source) && option.active);
    const organizationalUnit = this.units().find((unit) => unit.id === Number(value.responsibleUnits) && unit.active);
    if (!solutionType || !source || !organizationalUnit) return null;

    return {
      initiativeCode: this.initiativeCode,
      projectCode: this.provisionalCode,
      name: value.name.trim(),
      startDateIso: value.startDate,
      startDate: this.formatReviewDate(value.startDate),
      solutionType: solutionType.name.trim() || 'Sin información registrada',
      source: source.name.trim() || 'Sin información registrada',
      digitalComponent: value.digitalComponent,
      responsible: value.responsible.trim(),
      organizationalUnit: this.organizationalUnitLabel(organizationalUnit.acronym, organizationalUnit.name),
      description: value.description.trim(),
      keyResults: value.keyResults.trim(),
      registerProject: () => this.registerProject(),
    };
  }

  private organizationalUnitLabel(acronym: string, name: string): string {
    const normalizedAcronym = acronym.trim();
    const normalizedName = name.trim();
    if (!normalizedAcronym) return normalizedName || 'Sin información registrada';
    if (!normalizedName || normalizedAcronym.localeCompare(normalizedName, 'es', { sensitivity: 'accent' }) === 0) return normalizedAcronym;
    return `${normalizedAcronym} — ${normalizedName}`;
  }

  private formatReviewDate(value: string): string {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) return 'Sin información registrada';
    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const calendarDate = new Date(Date.UTC(year, month - 1, day, 12));
    if (calendarDate.getUTCFullYear() !== year || calendarDate.getUTCMonth() !== month - 1 || calendarDate.getUTCDate() !== day) {
      return 'Sin información registrada';
    }
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeZone: 'America/Lima' }).format(calendarDate);
  }

  private buildInput(): DerivedProjectInput {
    const value = this.form.getRawValue();
    return {
      initiativeCode: this.initiativeCode,
      code: this.provisionalCode,
      startDate: value.startDate,
      name: value.name,
      solutionTypeId: Number(value.solutionType),
      sourceId: Number(value.source),
      responsible: value.responsible,
      organizationalUnitId: Number(value.responsibleUnits),
      peiObjectiveId: value.peiObjective ? Number(value.peiObjective) : undefined,
      poiActivityId: value.poiActivity ? Number(value.poiActivity) : undefined,
      description: value.description,
      keyResults: value.keyResults,
      note: value.note,
      digitalComponent: value.digitalComponent as DerivedProjectInput['digitalComponent'],
    };
  }

  private activeId(option: { id: number; active: boolean } | null | undefined): string {
    return option?.active ? String(option.id) : '';
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
      && units.phase === 'ready' && units.value.length > 0;
  }

  private syncDisabled(control: AbstractControl, disabled: boolean): void {
    if (disabled && control.enabled) control.disable({ emitEvent: false });
    else if (!disabled && control.disabled) control.enable({ emitEvent: false });
  }
}
