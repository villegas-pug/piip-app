import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { clampPageIndex, paginationRange, totalPages } from './piip-pagination.utils';

@Component({
  selector: 'app-piip-pagination',
  imports: [MatIconModule],
  templateUrl: './piip-pagination.component.html',
  styleUrl: './piip-pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PiipPaginationComponent {
  readonly totalItems = input.required<number>();
  readonly pageIndex = input.required<number>();
  readonly itemLabel = input('registros');
  readonly pageChange = output<number>();

  readonly totalPages = computed(() => totalPages(this.totalItems()));
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.totalItems()));
  readonly range = computed(() => paginationRange(this.totalItems(), this.currentPage()));
  readonly rangeLabel = computed(() => {
    const range = this.range();
    return range.start === 0
      ? `Mostrando 0 de 0 ${this.itemLabel()}`
      : `Mostrando ${range.start}–${range.end} de ${this.totalItems()} ${this.itemLabel()}`;
  });

  changePage(pageIndex: number): void {
    const nextPage = clampPageIndex(pageIndex, this.totalItems());
    if (nextPage !== this.currentPage()) this.pageChange.emit(nextPage);
  }
}
