import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { PIIP_CATALOGS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PreexistingProjectInput } from '../../core/piip.models';

type DocumentField =
  | 'technicalOpinionReport'
  | 'formalApprovalDecision'
  | 'finalProductApprovalDocument'
  | 'projectManagementDocumentation'
  | 'finalClosureReport';

type DocumentMode = 'NOT_APPLICABLE' | 'FILE' | 'PENDING';

@Component({
  selector: 'app-preexisting-project-form',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './preexisting-project-form.component.html',
  styleUrl: './preexisting-project-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreexistingProjectFormComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  readonly catalogs = PIIP_CATALOGS;
  readonly catalogState = this.repository.catalogs;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly units = this.repository.organizationalUnits;
  readonly provisionalCode = `P-${String(this.repository.projects().length + 3).padStart(3, '0')}-2026`;
  readonly reviewOpen = signal(false);
  readonly submitting = signal(false);
  readonly documentFiles = signal<Record<DocumentField, File | null>>({
    technicalOpinionReport: null,
    formalApprovalDecision: null,
    finalProductApprovalDocument: null,
    projectManagementDocumentation: null,
    finalClosureReport: null,
  });

  readonly form = this.formBuilder.nonNullable.group({
    recordType: [{ value: 'Proyecto', disabled: true }],
    code: [{ value: this.provisionalCode, disabled: true }],
    originCode: [{ value: 'NA', disabled: true }],
    solutionType: [{ value: 'Definido por el backend', disabled: true }],
    status: [{ value: 'Proyecto en ejecución', disabled: true }],
    startDate: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(180)]],
    source: [{ value: '', disabled: this.catalogState().phase !== 'ready' }, Validators.required],
    responsible: ['', Validators.required],
    responsibleUnits: [{ value: '', disabled: this.unitsState().phase !== 'ready' }, Validators.required],
    peiObjective: [{ value: '', disabled: this.catalogState().phase !== 'ready' }],
    poiActivity: [{ value: '', disabled: this.catalogState().phase !== 'ready' }],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    keyResults: ['', Validators.maxLength(1000)],
    note: ['', Validators.maxLength(600)],
    digitalComponent: ['', Validators.required],
    technicalOpinionMode: ['NOT_APPLICABLE' as DocumentMode],
    formalApprovalMode: ['NOT_APPLICABLE' as DocumentMode],
    finalProductApprovalMode: ['NOT_APPLICABLE' as DocumentMode],
    projectManagementMode: ['PENDING' as DocumentMode],
    finalClosureMode: ['NOT_APPLICABLE' as DocumentMode],
  });

  constructor() {
    effect(() => {
      const catalogsReady = this.catalogState().phase === 'ready';
      const unitsReady = this.unitsState().phase === 'ready';
      this.syncDisabled(this.form.controls.source, !catalogsReady);
      this.syncDisabled(this.form.controls.peiObjective, !catalogsReady);
      this.syncDisabled(this.form.controls.poiActivity, !catalogsReady);
      this.syncDisabled(this.form.controls.responsibleUnits, !unitsReady);
      const catalogs = this.catalogState().value;
      this.reconcile(this.form.controls.source, catalogs.sources);
      this.reconcile(this.form.controls.peiObjective, catalogs.peiObjectives);
      this.reconcile(this.form.controls.poiActivity, catalogs.poiActivities);
      this.reconcile(this.form.controls.responsibleUnits, this.units());
    });
  }

  scrollTo(sectionId: string): void {
    this.elementRef.nativeElement.querySelector(`#${sectionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  onFileSelected(field: DocumentField, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.documentFiles.update((files) => ({ ...files, [field]: file }));
  }

  saveDraft(): void {
    const documentFiles = Object.fromEntries(Object.entries(this.documentFiles()).map(([field, file]) => [field, file?.name ?? null]));
    this.repository.savePreexistingProjectDraft({ ...this.form.getRawValue(), documentFiles });
    this.snackBar.open('Borrador guardado localmente. No es un estado oficial.', 'Cerrar', { duration: 3500 });
  }

  openReview(): void {
    this.form.markAllAsTouched();
    if (!this.dependenciesReady() || this.form.invalid || this.hasMissingSelectedFile()) {
      this.snackBar.open('Completa los campos requeridos y adjunta los archivos seleccionados.', 'Cerrar', { duration: 4200 });
      return;
    }
    this.reviewOpen.set(true);
  }

  async registerProject(): Promise<void> {
    if (this.submitting() || !this.dependenciesReady()) return;
    this.submitting.set(true);
    try {
      const record = await Promise.resolve(this.repository.registerPreexistingProject(this.buildRegistrationInput()));
      this.reviewOpen.set(false);
      this.snackBar.open(`Proyecto preexistente ${record.code} incorporado al portafolio.`, 'Cerrar', { duration: 3600 });
      await this.router.navigate(['/proyectos', record.code, 'documentos']);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No fue posible registrar el proyecto.';
      this.snackBar.open(message, 'Cerrar', { duration: 4200 });
    } finally {
      this.submitting.set(false);
    }
  }

  private hasMissingSelectedFile(): boolean {
    const value = this.form.getRawValue();
    const files = this.documentFiles();
    return [
      ['technicalOpinionReport', value.technicalOpinionMode],
      ['formalApprovalDecision', value.formalApprovalMode],
      ['finalProductApprovalDocument', value.finalProductApprovalMode],
      ['projectManagementDocumentation', value.projectManagementMode],
      ['finalClosureReport', value.finalClosureMode],
    ].some(([field, mode]) => mode === 'FILE' && !files[field as DocumentField]);
  }

  private buildRegistrationInput(): PreexistingProjectInput {
    const value = this.form.getRawValue();
    return {
      code: this.provisionalCode,
      name: value.name,
      startDate: value.startDate,
      sourceId: Number(value.source),
      responsible: value.responsible,
      organizationalUnitId: Number(value.responsibleUnits),
      peiObjectiveId: value.peiObjective ? Number(value.peiObjective) : undefined,
      poiActivityId: value.poiActivity ? Number(value.poiActivity) : undefined,
      description: value.description,
      keyResults: value.keyResults,
      note: value.note,
      digitalComponent: value.digitalComponent as 'Si' | 'No',
      technicalOpinionReport: this.documentValue('technicalOpinionReport', value.technicalOpinionMode),
      formalApprovalDecision: this.documentValue('formalApprovalDecision', value.formalApprovalMode),
      finalProductApprovalDocument: this.documentValue('finalProductApprovalDocument', value.finalProductApprovalMode),
      projectManagementDocumentation: this.documentValue('projectManagementDocumentation', value.projectManagementMode),
      finalClosureReport: this.documentValue('finalClosureReport', value.finalClosureMode),
      documentAttachments: [
        this.attachment('technicalOpinionReport', value.technicalOpinionMode, 'INITIATIVE_TECHNICAL_OPINION'),
        this.attachment('formalApprovalDecision', value.formalApprovalMode, 'FORMAL_APPROVAL_DECISION'),
        this.attachment('finalProductApprovalDocument', value.finalProductApprovalMode, 'FINAL_PRODUCT_APPROVAL'),
        this.attachment('projectManagementDocumentation', value.projectManagementMode, 'PROJECT_MANAGEMENT_DOCUMENTATION'),
        this.attachment('finalClosureReport', value.finalClosureMode, 'FINAL_CLOSURE_REPORT'),
      ],
    };
  }

  private documentValue(field: DocumentField, mode: DocumentMode): string {
    if (mode === 'NOT_APPLICABLE') return 'No Aplica';
    return this.documentFiles()[field]?.name ?? '';
  }

  private attachment(field: DocumentField, mode: DocumentMode, type: import('../../core/piip.models').DocumentType) {
    const documentTypeId = this.catalogState().value.documentTypes.find((item) => item.code === type && item.active)?.id;
    if (documentTypeId === undefined) throw new Error('El tipo documental seleccionado ya no está disponible. Recarga los catálogos.');
    return { type, documentTypeId, mode, file: this.documentFiles()[field] ?? undefined };
  }

  private reconcile(control: { value: string; setErrors(errors: Record<string, boolean> | null): void }, options: Array<{ id: number; active: boolean }>): void {
    if (!control.value) return;
    if (!options.some((option) => option.id === Number(control.value) && option.active)) control.setErrors({ unavailable: true });
    else if ((control as { errors?: Record<string, boolean> | null }).errors?.['unavailable']) control.setErrors(null);
  }

  private dependenciesReady(): boolean {
    const catalogs = this.catalogState();
    const units = this.unitsState();
    return catalogs.phase === 'ready' && catalogs.value.sources.length > 0
      && units.phase === 'ready' && units.value.length > 0;
  }

  private syncDisabled(control: AbstractControl, disabled: boolean): void {
    if (disabled && control.enabled) control.disable({ emitEvent: false });
    else if (!disabled && control.disabled) control.enable({ emitEvent: false });
  }
}
