import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PiipApiError, piipErrorStatus } from '../../core/piip-http.repository';
import type { AdministrativeExecutingUnit, AdministrativeOrganizationalUnit } from '../../core/piip.models';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { OrganizationalUnitStateDialogComponent } from './organizational-unit-state-dialog.component';

@Component({
  selector: 'app-organizational-unit-administration',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './organizational-unit-administration.component.html',
  styleUrl: './organizational-unit-administration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationalUnitAdministrationComponent {
  readonly repository = inject(PIIP_REPOSITORY);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly executingUnitId = Number(this.route.snapshot.paramMap.get('executingUnitId'));
  readonly executingUnit = signal<AdministrativeExecutingUnit | null>(null);
  readonly units = signal<AdministrativeOrganizationalUnit[]>([]);
  readonly loading = signal(true);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly liveMessage = signal('');
  readonly hasContext = computed(() => this.executingUnit() !== null);

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    this.units.set([]);
    try {
      if (!Number.isSafeInteger(this.executingUnitId) || this.executingUnitId <= 0) throw new PiipApiError(404, 'El contexto de la Unidad Ejecutora no es válido.');
      const context = await Promise.resolve(this.repository.loadAdministrativeExecutingUnit(this.executingUnitId));
      if (!context) throw new PiipApiError(404, 'La Unidad Ejecutora solicitada no existe dentro de tu ámbito.');
      this.executingUnit.set(context);
      const units = await Promise.resolve(this.repository.loadAdministrativeOrganizationalUnits(this.executingUnitId));
      this.units.set(units);
    } catch (error) {
      await this.handleError(error, 'No fue posible consultar las Unidades Orgánicas.');
    } finally {
      this.loading.set(false);
    }
  }

  openCreate(): void {
    if (!this.hasContext()) return;
    void this.router.navigate(['/administracion/unidades-ejecutoras', this.executingUnitId, 'unidades-organicas', 'nueva']);
  }

  openEdit(unit: AdministrativeOrganizationalUnit): void {
    void this.router.navigate(['/administracion/unidades-ejecutoras', this.executingUnitId, 'unidades-organicas', unit.id, 'editar']);
  }

  backToExecutingUnits(): void {
    const institutionId = this.executingUnit()?.institution.id;
    void this.router.navigate(['/administracion/unidades-ejecutoras'], institutionId ? { queryParams: { institutionId } } : undefined);
  }

  changeState(unit: AdministrativeOrganizationalUnit): void {
    if (this.pending()) return;
    this.dialog.open(OrganizationalUnitStateDialogComponent, {
      width: '500px',
      maxWidth: 'calc(100vw - 32px)',
      autoFocus: 'first-header',
      restoreFocus: true,
      data: { unit },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (confirmed) void this.persistState(unit);
    });
  }

  private async persistState(unit: AdministrativeOrganizationalUnit): Promise<void> {
    this.pending.set(true);
    this.liveMessage.set('Guardando cambio de estado de la Unidad Orgánica.');
    try {
      const updated = unit.active
        ? await Promise.resolve(this.repository.deactivateAdministrativeOrganizationalUnit(unit.id, unit.version))
        : await Promise.resolve(this.repository.reactivateAdministrativeOrganizationalUnit(unit.id, unit.version));
      this.units.update((items) => items.map((item) => item.id === updated.id ? updated : item));
      this.liveMessage.set(`Unidad Orgánica ${updated.active ? 'reactivada' : 'desactivada'}.`);
    } catch (error) {
      await this.handleError(error, 'No fue posible cambiar el estado de la Unidad Orgánica.');
    } finally {
      this.pending.set(false);
    }
  }

  private async handleError(error: unknown, fallback: string): Promise<void> {
    const status = piipErrorStatus(error);
    this.error.set(status === 403 ? 'No tienes autorización sobre esta Unidad Ejecutora.' : status === 404 ? 'La Unidad Ejecutora o el contexto solicitado no existe.' : status === 409 ? 'La información cambió. Recarga la lista antes de volver a intentarlo.' : status === 422 ? (error instanceof Error ? error.message : 'La operación no es compatible con el estado actual.') : fallback);
    this.liveMessage.set(this.error());
    if (status === 403 || status === 404) {
      this.executingUnit.set(null);
      this.units.set([]);
      this.backToExecutingUnits();
    }
  }
}
