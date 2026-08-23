import { ChangeDetectionStrategy, Component, HostListener, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import type { CatalogBundle, InitiativeDetail, InitiativeUpdateInput, PiipPortfolioRecord, PiipRecordType, ProjectDetail, ProjectUpdateInput } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { canEditInitiative, canEditProject } from '../../core/portfolio-edit-permissions';
import type { OrganizationalUnit } from '../../core/piip.models';
import type { PendingChangesAware } from '../../core/pending-changes.guard';

interface EditSnapshot {
  version: number;
  name: string;
  solutionTypeId: number | null;
  sourceId: number | null;
  startDate: string;
  responsible: string;
  peiObjectiveId: number | null;
  poiActivityId: number | null;
  responsibleUnitIds: number[];
  description: string;
  keyResults: string;
  note: string;
  digitalComponent: PiipPortfolioRecord['digitalComponent'];
}

type PortfolioRecordEditVariant = 'INITIATIVE' | 'DERIVED_PROJECT' | 'PREEXISTING_PROJECT';

@Component({
  selector: 'app-portfolio-record-edit',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './portfolio-record-edit.component.html',
  styleUrl: './portfolio-record-edit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PortfolioRecordEditComponent implements PendingChangesAware {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  private readonly paramMap = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  readonly recordType = computed(() => (this.routeData()['recordType'] as PiipRecordType | undefined) ?? 'Iniciativa');
  readonly code = computed(() => this.paramMap().get('code') ?? '');
  readonly detail = computed<InitiativeDetail | ProjectDetail | undefined>(() => this.recordType() === 'Iniciativa'
    ? this.repository.getInitiativeDetail(this.code())
    : this.repository.getProjectDetail(this.code()));
  readonly record = computed(() => this.detail()?.portfolioRecord);
  readonly variant = computed<PortfolioRecordEditVariant | null>(() => {
    if (this.recordType() === 'Iniciativa') return 'INITIATIVE';
    const detail = this.detail() as ProjectDetail | undefined;
    if (!detail) return null;
    return detail.project.originMode === 'PREEXISTING' ? 'PREEXISTING_PROJECT' : 'DERIVED_PROJECT';
  });
  readonly isPreexistingProject = computed(() => this.variant() === 'PREEXISTING_PROJECT');
  readonly catalogState = this.repository.catalogs;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly units = this.repository.organizationalUnits;
  readonly submitting = signal(false);
  readonly loading = signal(true);
  readonly conflict = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly baseline = signal<EditSnapshot | null>(null);
  readonly isEditable = computed(() => this.recordType() === 'Iniciativa'
    ? canEditInitiative(this.detail() as InitiativeDetail | undefined, this.repository.canAdministerExecutingUnit(this.record()?.executingUnitId))
    : canEditProject(this.detail() as ProjectDetail | undefined, this.repository.canAdministerExecutingUnit(this.record()?.executingUnitId)));

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(180)]],
    startDate: ['', Validators.required],
    solutionTypeId: [''],
    sourceId: [''],
    responsible: ['', Validators.required],
    peiObjectiveId: [''],
    poiActivityId: [''],
    responsibleUnitIds: this.formBuilder.nonNullable.control<number[]>([], Validators.minLength(1)),
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    keyResults: [''],
    note: [''],
    digitalComponent: ['', Validators.required],
  });

  private loadedKey = '';

  constructor() {
    effect(() => {
      const code = this.code();
      const recordType = this.recordType();
      if (!code) return;
      const requestKey = `${recordType}:${code}`;
      if (this.loadedKey !== requestKey) {
        this.loadedKey = requestKey;
        this.loading.set(true);
        void Promise.resolve(this.repository.reloadPortfolioRecord(recordType, code)).then(() => {
          const freshRecord = this.record();
          if (freshRecord) this.initialize(freshRecord);
          this.loading.set(false);
        }).catch((error) => {
          this.showError(error);
          this.loading.set(false);
        });
        return;
      }
      const record = this.record();
      if (!record) return;
      if (!this.baseline() || this.conflict()) this.initialize(record);
      this.loading.set(false);
    });
  }

  hasPendingChanges(): boolean {
    return this.form.dirty && !this.submitting();
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.hasPendingChanges()) return;
    event.preventDefault();
    event.returnValue = '';
  }

  setResponsibleUnitIds(ids: readonly number[]): void {
    if (this.hasMultipleResponsibleUnits()) return;
    this.form.controls.responsibleUnitIds.setValue(ids.length ? [ids[0]] : []);
    this.form.controls.responsibleUnitIds.markAsDirty();
  }

  setResponsibleUnitFromEvent(event: Event): void {
    const rawValue = (event.target as HTMLSelectElement).value;
    const id = Number(rawValue);
    this.setResponsibleUnitIds(Number.isInteger(id) && id > 0 ? [id] : []);
  }

  selectedResponsibleUnitId(): string {
    const ids = this.form.controls.responsibleUnitIds.value;
    return ids.length === 1 ? String(ids[0]) : '';
  }

  hasMultipleResponsibleUnits(): boolean {
    return (this.baseline()?.responsibleUnitIds.length ?? 0) > 1;
  }

  responsibleUnitOptions(): readonly OrganizationalUnit[] {
    const currentIds = this.baseline()?.responsibleUnitIds ?? [];
    const candidates = [...this.units(), ...(this.record()?.responsibleUnitReferences ?? [])];
    return candidates.filter((unit, index, all) =>
      (unit.active || currentIds.includes(unit.id)) && all.findIndex((candidate) => candidate.id === unit.id) === index,
    );
  }

  currentResponsibleUnits(): readonly OrganizationalUnit[] {
    const ids = this.baseline()?.responsibleUnitIds ?? this.form.controls.responsibleUnitIds.value;
    return ids.flatMap((id) => this.responsibleUnitOptions().filter((unit) => unit.id === id));
  }

  isHistorical(id: string, field: 'solutionTypeId' | 'sourceId' | 'peiObjectiveId' | 'poiActivityId'): boolean {
    if (!id) return false;
    const options = this.catalogOptions(field);
    return options.some((option) => option.id === Number(id) && !option.active);
  }

  isSelectableCatalogOption(option: { id: number; active: boolean }, field: 'solutionTypeId' | 'sourceId' | 'peiObjectiveId' | 'poiActivityId'): boolean {
    const currentValue = this.form.controls[field].value;
    return option.active || (currentValue !== '' && Number(currentValue) === option.id);
  }

  catalogOptions(field: 'solutionTypeId' | 'sourceId' | 'peiObjectiveId' | 'poiActivityId') {
    const catalogs: CatalogBundle = this.catalogState().value;
    if (field === 'solutionTypeId') return catalogs.solutionTypes;
    if (field === 'sourceId') return catalogs.sources;
    return field === 'peiObjectiveId' ? catalogs.peiObjectives : catalogs.poiActivities;
  }

  async save(): Promise<void> {
    if (this.submitting() || this.conflict() || !this.isEditable()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.errorMessage.set('Completa los campos requeridos y selecciona al menos una Unidad Orgánica responsable.');
      return;
    }
    const baseline = this.baseline();
    if (!baseline) return;
    const value = this.form.getRawValue();
    const body = this.sparseBody(value, baseline);
    if (Object.keys(body).length === 1) {
      this.errorMessage.set('No hay cambios efectivos para guardar.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const updated = this.recordType() === 'Iniciativa'
        ? await Promise.resolve(this.repository.updateInitiative(this.code(), body as unknown as InitiativeUpdateInput))
        : await Promise.resolve(this.repository.updateProject(this.code(), body as unknown as ProjectUpdateInput));
      this.form.markAsPristine();
      this.baseline.set(this.snapshot(updated));
      this.snackBar.open('Registro actualizado correctamente.', 'Cerrar', { duration: 4200 });
      await this.router.navigate([this.recordType() === 'Iniciativa' ? '/iniciativas' : '/proyectos', this.code()], { queryParams: { updated: '1' } });
    } catch (error) {
      this.showError(error);
    } finally {
      this.submitting.set(false);
    }
  }

  async reloadLatest(): Promise<void> {
    this.conflict.set(false);
    this.baseline.set(null);
    this.errorMessage.set(null);
    this.form.markAsPristine();
    this.loading.set(true);
    await Promise.resolve(this.repository.reloadPortfolioRecord(this.recordType(), this.code())).catch((error) => this.showError(error));
    const record = this.record();
    if (record) this.initialize(record);
    this.loading.set(false);
  }

  async cancel(): Promise<void> {
    await this.router.navigate([this.recordType() === 'Iniciativa' ? '/iniciativas' : '/proyectos', this.code()]);
  }

  formatDate(value: string): string {
    if (!value) return 'Sin información registrada';
    return new Intl.DateTimeFormat('es-PE').format(new Date(`${value}T00:00:00`));
  }

  formatDateTime(value?: string): string {
    if (!value) return 'Sin información registrada';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Sin información registrada';
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Lima',
    }).format(date);
  }

  private initialize(record: PiipPortfolioRecord): void {
    const snapshot = this.snapshot(record);
    this.baseline.set(snapshot);
    this.form.reset({
      name: snapshot.name, startDate: snapshot.startDate, solutionTypeId: this.asString(snapshot.solutionTypeId), sourceId: this.asString(snapshot.sourceId),
      responsible: snapshot.responsible, peiObjectiveId: this.asString(snapshot.peiObjectiveId), poiActivityId: this.asString(snapshot.poiActivityId),
      responsibleUnitIds: snapshot.responsibleUnitIds, description: snapshot.description, keyResults: snapshot.keyResults, note: snapshot.note,
      digitalComponent: snapshot.digitalComponent,
    });
    this.conflict.set(false);
  }

  private snapshot(record: PiipPortfolioRecord): EditSnapshot {
    return {
      version: record.version ?? 0, name: record.name, solutionTypeId: record.solutionTypeReference?.id ?? null, sourceId: record.sourceReference?.id ?? null,
      startDate: record.startDate, responsible: record.responsible, peiObjectiveId: record.peiObjectiveReference?.id ?? null, poiActivityId: record.poiActivityReference?.id ?? null,
      responsibleUnitIds: (record.responsibleUnitReferences ?? []).map((unit) => unit.id), description: record.description, keyResults: record.keyResults, note: record.note,
      digitalComponent: record.digitalComponent,
    };
  }

  private sparseBody(value: ReturnType<typeof this.form.getRawValue>, baseline: EditSnapshot): Record<string, unknown> {
    const body: Record<string, unknown> = { version: baseline.version };
    const maybe = (key: keyof EditSnapshot, valueToSend: unknown) => { if (valueToSend !== baseline[key]) body[key] = valueToSend; };
    maybe('name', value.name); maybe('startDate', value.startDate); maybe('responsible', value.responsible); maybe('description', value.description); maybe('keyResults', value.keyResults); maybe('note', value.note); maybe('digitalComponent', value.digitalComponent);
    if (!this.isPreexistingProject()) {
      const solutionTypeId = this.numberOrNull(value.solutionTypeId);
      if (solutionTypeId !== baseline.solutionTypeId) body['solutionTypeId'] = solutionTypeId;
    }
    const sourceId = this.numberOrNull(value.sourceId); if (sourceId !== baseline.sourceId) body['sourceId'] = sourceId;
    const peiObjectiveId = this.numberOrNull(value.peiObjectiveId); if (peiObjectiveId !== baseline.peiObjectiveId) body['peiObjectiveId'] = peiObjectiveId;
    const poiActivityId = this.numberOrNull(value.poiActivityId); if (poiActivityId !== baseline.poiActivityId) body['poiActivityId'] = poiActivityId;
    if (JSON.stringify(value.responsibleUnitIds) !== JSON.stringify(baseline.responsibleUnitIds)) body['responsibleUnitIds'] = [...value.responsibleUnitIds];
    return body;
  }

  private numberOrNull(value: string): number | null { return value ? Number(value) : null; }
  private asString(value: number | null): string { return value === null ? '' : String(value); }

  private showError(error: unknown): void {
    const status = typeof error === 'object' && error !== null && 'status' in error ? Number((error as { status?: number }).status) : 0;
    if (status === 409) {
      this.conflict.set(true);
      this.errorMessage.set('La versión abierta está desactualizada. Tus cambios locales se conservaron; recarga la versión vigente para continuar.');
      return;
    }
    this.conflict.set(false);
    if (status === 403) {
      this.errorMessage.set('No tienes permisos para actualizar este registro. Tus cambios locales se conservaron.');
      return;
    }
    if (status === 404) {
      this.errorMessage.set('El registro solicitado ya no existe o no está disponible. Tus cambios locales se conservaron.');
      return;
    }
    if (status === 422) {
      this.errorMessage.set(error instanceof Error ? error.message : 'La actualización fue rechazada por las reglas del registro. Tus cambios locales se conservaron.');
      return;
    }
    this.errorMessage.set(error instanceof Error ? error.message : 'No fue posible cargar o actualizar el registro.');
  }
}
