import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PendingChangesAware } from '../../core/pending-changes.guard';
import { PiipApiError, piipErrorStatus } from '../../core/piip-http.repository';
import type { AdministrativeExecutingUnit, AdministrativeInstitution } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

const nonNegativeInteger: ValidatorFn = (control): ValidationErrors | null => control.value === null || (Number.isInteger(control.value) && control.value >= 0)
  ? null
  : { nonNegativeInteger: true };
const nonBlank: ValidatorFn = (control): ValidationErrors | null => typeof control.value === 'string' && control.value.trim() ? null : { blank: true };

@Component({
  selector: 'app-executing-unit-form',
  imports: [MatButtonModule, MatIconModule, ReactiveFormsModule, RouterLink],
  templateUrl: './executing-unit-form.component.html',
  styleUrl: './executing-unit-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutingUnitFormComponent implements PendingChangesAware {
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly unit = signal<AdministrativeExecutingUnit | null>(null);
  readonly institution = signal<AdministrativeInstitution | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly liveMessage = signal('');
  readonly unitId = Number(this.route.snapshot.paramMap.get('id'));
  readonly editing = computed(() => Number.isSafeInteger(this.unitId) && this.unitId > 0);
  readonly form = this.formBuilder.group({
    institution: this.formBuilder.nonNullable.control({ value: '', disabled: true }),
    code: this.formBuilder.nonNullable.control({ value: '', disabled: true }),
    version: this.formBuilder.nonNullable.control({ value: '', disabled: true }),
    name: this.formBuilder.nonNullable.control('', [Validators.required, nonBlank, Validators.maxLength(180)]),
    displayOrder: this.formBuilder.control<number | null>(null, [Validators.min(0), nonNegativeInteger]),
    registeredAt: this.formBuilder.nonNullable.control({ value: '', disabled: true }),
    activatedAt: this.formBuilder.nonNullable.control({ value: '', disabled: true }),
  });

  constructor() {
    void this.load();
  }

  hasPendingChanges(): boolean { return this.form.dirty && !this.saving(); }
  confirmPendingChanges(): boolean { return window.confirm('Tienes cambios sin guardar. ¿Deseas salir sin guardarlos?'); }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const institutions = await Promise.resolve(this.repository.loadAdministrativeInstitutions());
      const requestedInstitutionId = Number(this.route.snapshot.queryParamMap.get('institutionId'));
      if (this.editing()) {
        const candidates = Number.isSafeInteger(requestedInstitutionId) && requestedInstitutionId > 0
          ? await Promise.resolve(this.repository.loadAdministrativeExecutingUnits(requestedInstitutionId))
          : (await Promise.all(institutions.map((item) => this.repository.loadAdministrativeExecutingUnits(item.id)))).flat();
        const unit = candidates.find((item) => item.id === this.unitId);
        if (!unit) throw new PiipApiError(404, 'La Unidad Ejecutora indicada no existe.');
        this.setUnit(unit);
      } else {
        const selected = institutions.find((item) => item.id === requestedInstitutionId);
        if (!selected) throw new PiipApiError(422, 'Selecciona una institución administrable antes de crear una Unidad Ejecutora.');
        this.institution.set(selected);
        this.form.controls.institution.setValue(`${selected.code} · ${selected.name}`);
        this.form.controls.displayOrder.clearValidators();
        this.form.controls.displayOrder.addValidators([Validators.min(0), nonNegativeInteger]);
        this.form.controls.displayOrder.updateValueAndValidity({ emitEvent: false });
      }
    } catch (error) {
      await this.handleError(error, 'No fue posible cargar el contexto de la Unidad Ejecutora.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (this.form.invalid || !this.institution()) {
      this.error.set('Completa el nombre y un orden no negativo cuando corresponda.');
      this.liveMessage.set(this.error());
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.liveMessage.set('Guardando Unidad Ejecutora.');
    try {
      const saved = this.editing()
        ? await Promise.resolve(this.repository.updateAdministrativeExecutingUnit(this.unitId, this.unit()!.version, { name: value.name.trim(), displayOrder: value.displayOrder as number }))
        : await Promise.resolve(this.repository.createAdministrativeExecutingUnit({ institutionId: this.institution()!.id, name: value.name.trim(), ...(value.displayOrder === null ? {} : { displayOrder: value.displayOrder }) }));
      this.unit.set(saved);
      this.form.markAsPristine();
      this.liveMessage.set(`Unidad Ejecutora ${saved.code} guardada correctamente.`);
      await this.returnToList(saved.institution.id);
    } catch (error) {
      await this.handleError(error, 'No fue posible guardar la Unidad Ejecutora.');
    } finally {
      this.saving.set(false);
    }
  }

  async reloadCurrent(): Promise<void> {
    await this.load();
    this.form.markAsPristine();
  }

  private setUnit(unit: AdministrativeExecutingUnit): void {
    this.unit.set(unit);
    this.institution.set(unit.institution);
    this.form.controls.institution.setValue(`${unit.institution.code} · ${unit.institution.name}`);
    this.form.controls.code.setValue(unit.code);
    this.form.controls.version.setValue(String(unit.version));
    this.form.controls.name.setValue(unit.name);
    this.form.controls.displayOrder.setValue(unit.displayOrder);
    this.form.controls.displayOrder.setValidators([Validators.required, Validators.min(0), nonNegativeInteger]);
    this.form.controls.displayOrder.updateValueAndValidity({ emitEvent: false });
    this.form.controls.registeredAt.setValue(unit.registeredAt);
    this.form.controls.activatedAt.setValue(unit.activatedAt);
    this.form.markAsPristine();
  }

  private async handleError(error: unknown, fallback: string): Promise<void> {
    const status = piipErrorStatus(error);
    const institutionId = this.institution()?.id;
    this.error.set(status === 403 ? 'No tienes autorización sobre la institución de esta Unidad Ejecutora.' : status === 404 ? 'La Unidad Ejecutora solicitada no existe.' : status === 409 ? 'La información cambió. El borrador se conservó; recarga explícitamente para obtener la versión vigente.' : status === 422 ? (error instanceof Error ? error.message : 'El contexto institucional no es válido.') : fallback);
    this.liveMessage.set(this.error());
    if (status === 403 || status === 404) {
      this.unit.set(null);
      this.institution.set(null);
    }
    if ((status === 403 || status === 404) && !this.saving()) await this.returnToList(institutionId);
  }

  private async returnToList(institutionId?: number): Promise<void> {
    await this.router.navigate(['/administracion/unidades-ejecutoras'], institutionId ? { queryParams: { institutionId } } : undefined);
  }
}
