import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import type { OrganizationalUnit } from '../../core/piip.models';

@Component({
  selector: 'app-responsible-unit-order-editor',
  standalone: true,
  templateUrl: './responsible-unit-order-editor.component.html',
  styleUrl: './responsible-unit-order-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResponsibleUnitOrderEditorComponent {
  @Input({ required: true }) units: readonly OrganizationalUnit[] = [];
  @Input() selectedIds: readonly number[] = [];
  @Output() readonly selectedIdsChange = new EventEmitter<number[]>();

  availableUnits(): readonly OrganizationalUnit[] {
    return this.units.filter((unit) => unit.active || this.selectedIds.includes(unit.id));
  }

  selectedUnits(): readonly OrganizationalUnit[] {
    return this.selectedIds.flatMap((id) => {
      const unit = this.units.find((candidate) => candidate.id === id);
      return unit ? [unit] : [];
    });
  }

  isSelected(id: number): boolean {
    return this.selectedIds.includes(id);
  }

  toggle(id: number, checked: boolean): void {
    const next = checked ? [...this.selectedIds, id] : this.selectedIds.filter((value) => value !== id);
    this.selectedIdsChange.emit(next);
  }

  remove(index: number): void {
    this.selectedIdsChange.emit(this.selectedIds.filter((_, position) => position !== index));
  }

  move(index: number, delta: -1 | 1): void {
    const target = index + delta;
    if (target < 0 || target >= this.selectedIds.length) return;
    const next = [...this.selectedIds];
    [next[index], next[target]] = [next[target], next[index]];
    this.selectedIdsChange.emit(next);
  }

  trackById(_index: number, unit: OrganizationalUnit): number {
    return unit.id;
  }
}
