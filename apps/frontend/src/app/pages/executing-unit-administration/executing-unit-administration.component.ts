import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { piipErrorStatus } from '../../core/piip-http.repository';
import type { AdministrativeExecutingUnit, AdministrativeInstitution } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { ExecutingUnitStateDialogComponent } from './executing-unit-state-dialog.component';

@Component({
  selector: 'app-executing-unit-administration',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './executing-unit-administration.component.html',
  styleUrl: './executing-unit-administration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutingUnitAdministrationComponent {
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly institutions = signal<AdministrativeInstitution[]>([]);
  readonly units = signal<AdministrativeExecutingUnit[]>([]);
  readonly selectedInstitutionId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly liveMessage = signal('');
  readonly selectedInstitution = computed(() => this.institutions().find((item) => item.id === this.selectedInstitutionId()));
  readonly canList = computed(() => this.selectedInstitutionId() !== null);

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const institutions = await Promise.resolve(this.repository.loadAdministrativeInstitutions());
      this.institutions.set(institutions);
      const requested = Number(this.route.snapshot.queryParamMap.get('institutionId'));
      const initial = Number.isSafeInteger(requested) && institutions.some((item) => item.id === requested)
        ? requested
        : institutions.length === 1 ? institutions[0].id : null;
      this.selectedInstitutionId.set(initial);
      if (initial !== null) await this.loadUnits(initial);
    } catch (error) {
      this.handleError(error, 'No fue posible consultar las instituciones administrables.');
    } finally {
      this.loading.set(false);
    }
  }

  async selectInstitution(value: string): Promise<void> {
    const institutionId = Number(value);
    if (!Number.isSafeInteger(institutionId) || !this.institutions().some((item) => item.id === institutionId)) {
      this.selectedInstitutionId.set(null);
      this.units.set([]);
      return;
    }
    this.selectedInstitutionId.set(institutionId);
    await this.router.navigate([], { relativeTo: this.route, queryParams: { institutionId }, queryParamsHandling: 'merge' });
    await this.loadUnits(institutionId);
  }

  openCreate(): void {
    const institutionId = this.selectedInstitutionId();
    if (institutionId === null) return;
    void this.router.navigate(['/administracion/unidades-ejecutoras/nueva'], { queryParams: { institutionId } });
  }

  openEdit(unit: AdministrativeExecutingUnit): void {
    void this.router.navigate(['/administracion/unidades-ejecutoras', unit.id, 'editar'], {
      queryParams: { institutionId: unit.institution.id },
    });
  }

  openOrganizationalUnits(unit: AdministrativeExecutingUnit): void {
    void this.router.navigate(['/administracion/unidades-ejecutoras', unit.id, 'unidades-organicas']);
  }

  changeState(unit: AdministrativeExecutingUnit): void {
    if (this.pending()) return;
    this.dialog.open(ExecutingUnitStateDialogComponent, {
      width: '500px',
      maxWidth: 'calc(100vw - 32px)',
      autoFocus: 'first-header',
      restoreFocus: true,
      data: { unit },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (confirmed) void this.persistState(unit);
    });
  }

  private async persistState(unit: AdministrativeExecutingUnit): Promise<void> {
    this.pending.set(true);
    this.liveMessage.set('Guardando cambio de estado de la Unidad Ejecutora.');
    try {
      const updated = unit.active
        ? await Promise.resolve(this.repository.deactivateAdministrativeExecutingUnit(unit.id, unit.version))
        : await Promise.resolve(this.repository.reactivateAdministrativeExecutingUnit(unit.id, unit.version));
      this.units.update((items) => items.map((item) => item.id === updated.id ? updated : item));
      this.liveMessage.set(`Unidad Ejecutora ${updated.active ? 'reactivada' : 'desactivada'}.`);
    } catch (error) {
      this.handleError(error, 'No fue posible cambiar el estado de la Unidad Ejecutora.');
    } finally {
      this.pending.set(false);
    }
  }

  private async loadUnits(institutionId: number): Promise<void> {
    this.loading.set(true);
    try {
      const units = await Promise.resolve(this.repository.loadAdministrativeExecutingUnits(institutionId));
      this.units.set([...units].sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name) || a.id - b.id));
    } catch (error) {
      this.units.set([]);
      this.handleError(error, 'No fue posible consultar las Unidades Ejecutoras.');
    } finally {
      this.loading.set(false);
    }
  }

  private handleError(error: unknown, fallback: string): void {
    const status = piipErrorStatus(error);
    this.error.set(status === 403 ? 'No tienes autorización sobre esta institución.' : status === 404 ? 'La institución solicitada no existe.' : fallback);
    this.liveMessage.set(this.error());
    if (status === 403 || status === 404) this.units.set([]);
  }
}
