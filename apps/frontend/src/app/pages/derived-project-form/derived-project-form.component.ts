import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PIIP_CATALOGS, RESPONSIBLE_UNITS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { DerivedProjectInput } from '../../core/piip.models';

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
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly units = computed(() => this.repository.organizationalUnits().length
    ? this.repository.organizationalUnits().map((unit) => unit.acronym || unit.name)
    : RESPONSIBLE_UNITS);
  readonly initiativeCode = this.route.snapshot.paramMap.get('initiativeCode') ?? '';
  readonly detail = this.repository.getInitiativeDetail(this.initiativeCode);
  readonly existingProject = this.repository.getProjectByOrigin(this.initiativeCode);
  readonly provisionalCode = this.repository.getNextProjectCode(this.initiativeCode);
  readonly reviewOpen = signal(false);
  readonly submitting = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    recordType: [{ value: 'Proyecto', disabled: true }],
    code: [{ value: this.provisionalCode, disabled: true }],
    originCode: [{ value: this.initiativeCode, disabled: true }],
    status: [{ value: 'Proyecto en ejecución', disabled: true }],
    startDate: ['', Validators.required],
    name: [this.detail?.portfolioRecord.name ?? '', [Validators.required, Validators.maxLength(180)]],
    solutionType: [this.detail?.portfolioRecord.solutionType ?? '', Validators.required],
    source: [this.detail?.portfolioRecord.source ?? '', Validators.required],
    responsible: [this.detail?.portfolioRecord.responsible ?? '', Validators.required],
    responsibleUnits: [this.detail?.portfolioRecord.responsibleUnits ?? '', Validators.required],
    peiObjective: [this.detail?.portfolioRecord.peiObjective ?? ''],
    poiActivity: [this.detail?.portfolioRecord.poiActivity ?? ''],
    description: [this.detail?.portfolioRecord.description ?? '', [Validators.required, Validators.maxLength(1000)]],
    keyResults: ['', Validators.maxLength(1000)],
    note: ['', Validators.maxLength(600)],
    digitalComponent: [this.detail?.portfolioRecord.digitalComponent ?? '', Validators.required],
  });

  scrollTo(sectionId: string): void {
    this.elementRef.nativeElement.querySelector(`#${sectionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  saveDraft(): void {
    this.repository.saveDerivedProjectDraft(this.form.getRawValue());
    this.snackBar.open('Borrador del proyecto guardado localmente.', 'Cerrar', { duration: 3300 });
  }

  openReview(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.snackBar.open('Completa los campos requeridos antes de revisar el proyecto.', 'Cerrar', { duration: 4000 });
      return;
    }
    this.reviewOpen.set(true);
  }

  async registerProject(): Promise<void> {
    if (this.submitting()) return;
    this.submitting.set(true);
    try {
      const record = await Promise.resolve(this.repository.registerDerivedProject(this.buildInput()));
      this.reviewOpen.set(false);
      this.snackBar.open('Proyecto derivado registrado y vinculado con su iniciativa.', 'Cerrar', { duration: 3800 });
      await this.router.navigate(['/proyectos', record.code, 'documentos']);
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible registrar el proyecto.', 'Cerrar', { duration: 4300 });
    } finally {
      this.submitting.set(false);
    }
  }

  private buildInput(): DerivedProjectInput {
    const value = this.form.getRawValue();
    return {
      initiativeCode: this.initiativeCode,
      code: this.provisionalCode,
      startDate: value.startDate,
      name: value.name,
      solutionType: value.solutionType as DerivedProjectInput['solutionType'],
      source: value.source,
      responsible: value.responsible,
      responsibleUnits: value.responsibleUnits,
      peiObjective: value.peiObjective,
      poiActivity: value.poiActivity,
      description: value.description,
      keyResults: value.keyResults,
      note: value.note,
      digitalComponent: value.digitalComponent as DerivedProjectInput['digitalComponent'],
    };
  }
}
