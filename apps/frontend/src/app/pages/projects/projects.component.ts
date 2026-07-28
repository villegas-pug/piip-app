import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';
import { PIIP_CATALOGS, RESPONSIBLE_UNITS } from '../../core/piip.catalogs';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipStatus } from '../../core/piip.models';
import {
  ProjectRegistrationDialogComponent,
  ProjectRegistrationDialogResult,
  ProjectRegistrationDialogView,
} from './project-registration-dialog.component';

@Component({
  selector: 'app-projects',
  imports: [ReactiveFormsModule, RouterLink, MatIconModule, MatMenuModule],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectsComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly catalogs = PIIP_CATALOGS;
  readonly units = RESPONSIBLE_UNITS;
  readonly filters = this.formBuilder.nonNullable.group({ search: '', status: 'Todos', unit: 'Todas', digital: 'Todos' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly filteredProjects = computed(() => {
    const value = this.filterValue();
    const search = (value.search ?? '').toLocaleLowerCase().trim();
    return this.repository.projects().filter((project) =>
      (!search || `${project.code} ${project.name}`.toLocaleLowerCase().includes(search)) &&
      (value.status === 'Todos' || project.status === value.status) &&
      (value.unit === 'Todas' || project.unit === value.unit) &&
      (value.digital === 'Todos' || project.digitalComponent === value.digital),
    );
  });

  resetFilters(): void {
    this.filters.reset({ search: '', status: 'Todos', unit: 'Todas', digital: 'Todos' });
  }

  openProjectRegistration(initialView: ProjectRegistrationDialogView): void {
    if (this.repository.role() !== 'Administrador PIIP') return;

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

  statusClass(status: PiipStatus): string {
    if (status === 'Proyecto en ejecución') return 'running';
    if (status === 'Producto aprobado') return 'product';
    if (status === 'Suspendido') return 'suspended';
    if (status === 'Finalizado') return 'finalized';
    if (status === 'Cancelado') return 'cancelled';
    return '';
  }
}
