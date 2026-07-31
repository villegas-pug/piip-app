import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink } from '@angular/router';
import { PIIP_CATALOGS, RESPONSIBLE_UNITS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipStatus } from '../../core/piip.models';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';

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
  readonly catalogs = PIIP_CATALOGS;
  readonly units = RESPONSIBLE_UNITS;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', status: 'Todos', source: 'Todos', unit: 'Todas', date: '' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly pageIndex = signal(0);

  readonly filteredInitiatives = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').trim().toLocaleLowerCase();
    return this.repository.initiatives().filter((initiative) =>
      (!search || `${initiative.code} ${initiative.name}`.toLocaleLowerCase().includes(search)) &&
      (value.status === 'Todos' || initiative.status === value.status) &&
      (value.source === 'Todos' || initiative.source === value.source) &&
      (value.unit === 'Todas' || initiative.unit === value.unit),
    );
  });

  readonly appliedFilterCount = computed(() => {
    const value = this.filterValue();
    return Number(Boolean(value.search)) + Number(Boolean(value.status && value.status !== 'Todos')) + Number(Boolean(value.source && value.source !== 'Todos')) + Number(Boolean(value.unit && value.unit !== 'Todas')) + Number(Boolean(value.date));
  });
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.filteredInitiatives().length));
  readonly pagedInitiatives = computed(() => paginateItems(this.filteredInitiatives(), this.currentPage()));

  constructor() {
    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.pageIndex.set(0));
  }

  resetFilters(): void {
    this.filters.reset({ search: '', status: 'Todos', source: 'Todos', unit: 'Todas', date: '' });
  }

  statusClass(status: PiipStatus): string {
    if (status === 'Iniciativa aprobada') return 'approved';
    if (status === 'Iniciativa archivada') return 'archived';
    if (status === 'No Admisible' || status === 'No Aplicable') return 'rejected';
    return '';
  }
}
