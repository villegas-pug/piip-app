import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PendingChangesAware } from '../../core/pending-changes.guard';
import { PiipApiError, piipErrorStatus } from '../../core/piip-http.repository';
import type { AdministrativeExecutingUnit, AdministrativeOrganizationalUnit } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

const nonBlank: ValidatorFn = (control): ValidationErrors | null => typeof control.value === 'string' && control.value.trim() ? null : { blank: true };

@Component({
  selector: 'app-organizational-unit-form',
  imports: [MatButtonModule, MatIconModule, ReactiveFormsModule, RouterLink],
  templateUrl: './organizational-unit-form.component.html',
  styleUrl: './organizational-unit-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationalUnitFormComponent implements PendingChangesAware {
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly executingUnitId = Number(this.route.snapshot.paramMap.get('executingUnitId'));
  readonly unitId = Number(this.route.snapshot.paramMap.get('id'));
  readonly editing = computed(() => Number.isSafeInteger(this.unitId) && this.unitId > 0);
  readonly executingUnit = signal<AdministrativeExecutingUnit | null>(null);
  readonly unit = signal<AdministrativeOrganizationalUnit | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly liveMessage = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    code: [{ value: '', disabled: true }],
    name: ['', [Validators.required, nonBlank, Validators.maxLength(180)]],
    acronym: ['', [Validators.required, nonBlank, Validators.maxLength(30)]],
    active: [true],
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
      if (!Number.isSafeInteger(this.executingUnitId) || this.executingUnitId <= 0) throw new PiipApiError(404, 'El contexto de la Unidad Ejecutora no es válido.');
      const context = await Promise.resolve(this.repository.loadAdministrativeExecutingUnit(this.executingUnitId));
      if (!context) throw new PiipApiError(404, 'La Unidad Ejecutora indicada no existe dentro de tu ámbito.');
      this.executingUnit.set(context);
      if (this.editing()) {
        const units = await Promise.resolve(this.repository.loadAdministrativeOrganizationalUnits(this.executingUnitId));
        const unit = units.find((item) => item.id === this.unitId);
        if (!unit) throw new PiipApiError(404, 'La Unidad Orgánica indicada no existe dentro de la Unidad Ejecutora.');
        this.setUnit(unit);
      }
    } catch (error) {
      await this.handleError(error, 'No fue posible cargar el contexto de la Unidad Orgánica.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.executingUnit()) {
      this.error.set('El nombre y la sigla son obligatorios y no pueden estar vacíos.');
      this.liveMessage.set(this.error());
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.liveMessage.set('Guardando Unidad Orgánica.');
    try {
      const saved = this.editing()
        ? await Promise.resolve(this.repository.updateAdministrativeOrganizationalUnit(this.unitId, this.unit()!.version, { name: value.name.trim(), acronym: value.acronym.trim() }))
        : await Promise.resolve(this.repository.createAdministrativeOrganizationalUnit({ executingUnitId: this.executingUnitId, name: value.name.trim(), acronym: value.acronym.trim(), active: value.active }));
      this.unit.set(saved);
      this.form.markAsPristine();
      this.liveMessage.set(`Unidad Orgánica ${saved.code} guardada correctamente.`);
      await this.router.navigate(['/administracion/unidades-ejecutoras', saved.executingUnit.id, 'unidades-organicas']);
    } catch (error) {
      await this.handleError(error, 'No fue posible guardar la Unidad Orgánica.');
    } finally {
      this.saving.set(false);
    }
  }

  async reloadCurrent(): Promise<void> {
    await this.load();
    this.form.markAsPristine();
  }

  private setUnit(unit: AdministrativeOrganizationalUnit): void {
    this.unit.set(unit);
    this.form.controls.code.setValue(unit.code);
    this.form.controls.name.setValue(unit.name);
    this.form.controls.acronym.setValue(unit.acronym);
    this.form.controls.active.setValue(unit.active);
    this.form.controls.active.disable({ emitEvent: false });
    this.form.markAsPristine();
  }

  private async handleError(error: unknown, fallback: string): Promise<void> {
    const status = piipErrorStatus(error);
    this.error.set(status === 403 ? 'No tienes autorización sobre esta Unidad Ejecutora.' : status === 404 ? 'La Unidad Orgánica o su contexto no existe.' : status === 409 ? 'La información cambió. El borrador se conservó; recarga explícitamente para obtener la versión vigente.' : status === 422 ? (error instanceof Error ? error.message : 'La operación no es compatible con el estado actual.') : fallback);
    this.liveMessage.set(this.error());
    if ((status === 403 || status === 404) && !this.saving()) await this.returnToList();
  }

  private async returnToList(): Promise<void> {
    if (Number.isSafeInteger(this.executingUnitId) && this.executingUnitId > 0) await this.router.navigate(['/administracion/unidades-ejecutoras', this.executingUnitId, 'unidades-organicas']);
    else await this.router.navigate(['/administracion/unidades-ejecutoras']);
  }
}
