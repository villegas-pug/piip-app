import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { OrganizationalUnit } from '../../core/piip.models';

/** Resumen reutilizable, compacto y ordenado de Unidades Orgánicas involucradas. */
@Component({
  selector: 'app-organizational-unit-summary',
  templateUrl: './organizational-unit-summary.component.html',
  styleUrl: './organizational-unit-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationalUnitSummaryComponent {
  readonly units = input<readonly OrganizationalUnit[]>([]);
  readonly label = input('Unidades Orgánicas Involucradas');
  readonly showHeader = input(true);
  readonly emptyText = input('Sin información registrada.');
}
