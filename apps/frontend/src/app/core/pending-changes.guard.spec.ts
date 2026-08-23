import { pendingChangesGuard } from './pending-changes.guard';

describe('pendingChangesGuard', () => {
  it('allows clean components without a browser prompt', () => {
    expect(pendingChangesGuard({ hasPendingChanges: () => false } as never, {} as never, {} as never, {} as never)).toBe(true);
  });
});
