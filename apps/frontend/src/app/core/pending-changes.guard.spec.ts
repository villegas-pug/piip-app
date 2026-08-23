import { pendingChangesGuard } from './pending-changes.guard';

describe('pendingChangesGuard', () => {
  it('allows clean components without a browser prompt', () => {
    const confirmPendingChanges = vi.fn();

    expect(pendingChangesGuard({ hasPendingChanges: () => false, confirmPendingChanges } as never, {} as never, {} as never, {} as never)).toBe(true);
    expect(confirmPendingChanges).not.toHaveBeenCalled();
  });

  it('delegates the pending navigation decision to the component', () => {
    const nativeConfirm = vi.spyOn(window, 'confirm');
    const decision = Promise.resolve(false);
    const confirmPendingChanges = vi.fn().mockReturnValue(decision);

    expect(pendingChangesGuard({ hasPendingChanges: () => true, confirmPendingChanges } as never, {} as never, {} as never, {} as never)).toBe(decision);
    expect(confirmPendingChanges).toHaveBeenCalledOnce();
    expect(nativeConfirm).not.toHaveBeenCalled();
  });
});
