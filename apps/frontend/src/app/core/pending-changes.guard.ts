import { CanDeactivateFn } from '@angular/router';
import type { Observable } from 'rxjs';

export interface PendingChangesAware {
  hasPendingChanges(): boolean;
  confirmPendingChanges(): boolean | Promise<boolean> | Observable<boolean>;
}

export const pendingChangesGuard: CanDeactivateFn<PendingChangesAware> = (component) => {
  if (!component.hasPendingChanges()) return true;
  return component.confirmPendingChanges();
};
