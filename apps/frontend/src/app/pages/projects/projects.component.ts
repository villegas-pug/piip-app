import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';
import { PIIP_CATALOGS, PROJECT_STATUSES } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipStatus } from '../../core/piip.models';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';
import {
  ProjectRegistrationDialogComponent,
  ProjectRegistrationDialogResult,
  ProjectRegistrationDialogView,
} from './project-registration-dialog.component';

type StatusTone = 'pending' | 'success' | 'progress' | 'neutral' | 'warning' | 'danger';

interface StatusVisual {
  readonly icon: string;
  readonly tone: StatusTone;
}

const STATUS_VISUALS: Readonly<Record<string, StatusVisual>> = {
  'Producto aprobado': { icon: 'check_circle', tone: 'success' },
  Finalizado: { icon: 'check_circle', tone: 'success' },
  'Proyecto en ejecución': { icon: 'play_circle', tone: 'progress' },
  Suspendido: { icon: 'pause_circle', tone: 'warning' },
  'Producto no aprobado': { icon: 'cancel', tone: 'danger' },
  Cancelado: { icon: 'cancel', tone: 'danger' },
};

const FALLBACK_STATUS_VISUAL: StatusVisual = { icon: 'circle', tone: 'neutral' };

@Component({
  selector: 'app-projects',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule, MatMenuModule, PiipPaginationComponent],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectsComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly projectStatuses = PROJECT_STATUSES;
  readonly units = this.repository.organizationalUnits;
  readonly unitsState = this.repository.organizationalUnitsState;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', status: 'Todos', unit: 'Todas', digital: 'Todos' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly pageIndex = signal(0);
  readonly filteredProjects = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').toLocaleLowerCase().trim();
    return this.repository.projects().filter((project) =>
      (!search || `${project.code} ${project.name}`.toLocaleLowerCase().includes(search)) &&
      (value.status === 'Todos' || project.status === value.status) &&
      (value.unit === 'Todas' || project.organizationalUnits?.some((unit) => unit.id === Number(value.unit))) &&
      (value.digital === 'Todos' || project.digitalComponent === value.digital),
    );
  });
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.filteredProjects().length));
  readonly pagedProjects = computed(() => paginateItems(this.filteredProjects(), this.currentPage()));
  readonly canCreateInActiveScope = computed(() => this.repository.canAdministerExecutingUnit(this.repository.selectedExecutingUnitId()));

  constructor() {
    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.pageIndex.set(0));
  }

  resetFilters(): void {
    this.filters.reset({ search: '', status: 'Todos', unit: 'Todas', digital: 'Todos' });
  }

  openProjectRegistration(initialView: ProjectRegistrationDialogView): void {
    if (!this.canCreateInActiveScope()) return;

    this.dialog.open(ProjectRegistrationDialogComponent, {
      width: '760px',
      maxWidth: 'calc(100vw - 32px)',
      maxHeight: 'calc(100vh - 32px)',
      autoFocus: 'first-heading',
      restoreFocus: true,
      panelClass: 'piip-registration-dialog',
      data: { initialView },
    }).afterClosed().subscribe((result: ProjectRegistrationDialogResult | undefined) => {
      if (!result) return;
      if (result.mode === 'PREEXISTING') {
        void this.router.navigate(['/proyectos/nuevo/preexistente']);
        return;
      }
      void this.router.navigate(['/proyectos/nuevo/derivado', result.initiativeCode]);
    });
  }

  projectCount(status: PiipStatus): number {
    return this.repository.projects().filter((project) => project.status === status).length;
  }

  canAdminister(project: { executingUnitId?: number }): boolean {
    return this.repository.canAdministerExecutingUnit(project.executingUnitId);
  }

  statusVisual(status: PiipStatus | string): StatusVisual { return STATUS_VISUALS[status] ?? FALLBACK_STATUS_VISUAL; }
}
