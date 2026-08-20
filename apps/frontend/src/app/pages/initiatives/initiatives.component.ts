import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink } from '@angular/router';
import { INITIATIVE_STATUSES } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipStatus } from '../../core/piip.models';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';
import { initiativeStatusVisual, type InitiativeStatusVisual } from './initiative-status-visual';

@Component({
  selector: 'app-initiatives',
  imports: [ReactiveFormsModule, MatIconModule, MatMenuModule, RouterLink, PiipPaginationComponent],
  templateUrl: './initiatives.component.html',
  styleUrl: './initiatives.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InitiativesComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogState = this.repository.catalogs;
  readonly initiativeStatuses = INITIATIVE_STATUSES;
  readonly units = this.repository.organizationalUnits;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', status: 'Todos', source: [{ value: 'Todos', disabled: this.catalogState().phase !== 'ready' }], unit: 'Todas', date: '' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly pageIndex = signal(0);

  readonly filteredInitiatives = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').trim().toLocaleLowerCase();
    const source = value.source ?? 'Todos';
    return this.repository.initiatives().filter((initiative) =>
      (!search || `${initiative.code} ${initiative.name}`.toLocaleLowerCase().includes(search)) &&
      (value.status === 'Todos' || initiative.status === value.status) &&
      (source === 'Todos' || initiative.sourceReference?.id === Number(source)) &&
      (value.unit === 'Todas' || initiative.organizationalUnits?.some((unit) => unit.id === Number(value.unit))),
    );
  });

  readonly appliedFilterCount = computed(() => {
    const value = this.filterValue();
    return Number(Boolean(value.search)) + Number(Boolean(value.status && value.status !== 'Todos')) + Number(Boolean(value.source && value.source !== 'Todos')) + Number(Boolean(value.unit && value.unit !== 'Todas')) + Number(Boolean(value.date));
  });
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.filteredInitiatives().length));
  readonly pagedInitiatives = computed(() => paginateItems(this.filteredInitiatives(), this.currentPage()));
  readonly canCreateInActiveScope = computed(() => this.repository.canAdministerExecutingUnit(this.repository.selectedExecutingUnitId()));

  canAdminister(initiative: { executingUnitId?: number }): boolean {
    return this.repository.canAdministerExecutingUnit(initiative.executingUnitId);
  }

  constructor() {
    effect(() => this.syncSourceFilterDisabled(this.catalogState().phase !== 'ready'));
    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.pageIndex.set(0));
  }

  resetFilters(): void {
    this.filters.reset({ search: '', status: 'Todos', source: 'Todos', unit: 'Todas', date: '' });
  }

  statusVisual(status: PiipStatus): InitiativeStatusVisual { return initiativeStatusVisual(status); }

  private syncSourceFilterDisabled(disabled: boolean): void {
    const control = this.filters.controls.source;
    if (disabled && control.enabled) control.disable({ emitEvent: false });
    else if (!disabled && control.disabled) control.enable({ emitEvent: false });
  }
}
