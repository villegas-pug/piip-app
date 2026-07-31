export const PIIP_PAGE_SIZE = 5;

export interface PaginationRange {
  start: number;
  end: number;
}

export function totalPages(totalItems: number, pageSize = PIIP_PAGE_SIZE): number {
  return Math.max(1, Math.ceil(Math.max(totalItems, 0) / pageSize));
}

export function clampPageIndex(pageIndex: number, totalItems: number, pageSize = PIIP_PAGE_SIZE): number {
  return Math.min(Math.max(pageIndex, 0), totalPages(totalItems, pageSize) - 1);
}

export function paginateItems<T>(items: readonly T[], pageIndex: number, pageSize = PIIP_PAGE_SIZE): readonly T[] {
  const currentPage = clampPageIndex(pageIndex, items.length, pageSize);
  const start = currentPage * pageSize;
  return items.slice(start, start + pageSize);
}

export function paginationRange(totalItems: number, pageIndex: number, pageSize = PIIP_PAGE_SIZE): PaginationRange {
  if (totalItems <= 0) return { start: 0, end: 0 };
  const currentPage = clampPageIndex(pageIndex, totalItems, pageSize);
  const start = currentPage * pageSize + 1;
  return { start, end: Math.min(start + pageSize - 1, totalItems) };
}
