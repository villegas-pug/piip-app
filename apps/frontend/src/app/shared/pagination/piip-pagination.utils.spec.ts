import { clampPageIndex, paginateItems, paginationRange, totalPages } from './piip-pagination.utils';

describe('PIIP pagination helpers', () => {
  it('keeps five records per page and returns a partial last page', () => {
    const records = Array.from({ length: 12 }, (_, index) => index + 1);

    expect(paginateItems(records, 0)).toEqual([1, 2, 3, 4, 5]);
    expect(paginateItems(records, 2)).toEqual([11, 12]);
    expect(totalPages(records.length)).toBe(3);
    expect(paginationRange(records.length, 2)).toEqual({ start: 11, end: 12 });
  });

  it('reports an empty range and clamps pages after the result set shrinks', () => {
    expect(paginationRange(0, 0)).toEqual({ start: 0, end: 0 });
    expect(totalPages(0)).toBe(1);
    expect(clampPageIndex(3, 6)).toBe(1);
  });
});
