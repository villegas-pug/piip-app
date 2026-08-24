import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'piip-authorization-recovery';

interface RecoveryState {
  message: string;
}

/** Mantiene visible y reintentable un cierre fail-closed entre navegaciones. */
@Injectable({ providedIn: 'root' })
export class AuthorizationRecoveryService {
  readonly active = signal(false);
  readonly message = signal('');
  readonly retrying = signal(false);

  private retryHandler?: () => Promise<void>;

  constructor() {
    this.restore();
  }

  setRetryHandler(handler: () => Promise<void>): void {
    this.retryHandler = handler;
  }

  async enter(message: string, retryHandler?: () => Promise<void>): Promise<void> {
    if (retryHandler) this.retryHandler = retryHandler;
    this.message.set(message);
    this.active.set(true);
    this.persist({ message });
  }

  async retry(): Promise<boolean> {
    if (this.retrying() || !this.retryHandler) return false;
    this.retrying.set(true);
    try {
      await this.retryHandler();
      this.clear();
      return true;
    } catch {
      this.message.set('No fue posible rehidratar tu autorización. Puedes volver a intentarlo.');
      this.active.set(true);
      this.persist({ message: this.message() });
      return false;
    } finally {
      this.retrying.set(false);
    }
  }

  clear(): void {
    this.active.set(false);
    this.message.set('');
    this.removePersisted();
  }

  private restore(): void {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const state = JSON.parse(raw) as Partial<RecoveryState>;
      if (typeof state.message !== 'string' || !state.message) return;
      this.message.set(state.message);
      this.active.set(true);
    } catch {
      this.removePersisted();
    }
  }

  private persist(state: RecoveryState): void {
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch { /* almacenamiento no disponible */ }
  }

  private removePersisted(): void {
    try { sessionStorage.removeItem(STORAGE_KEY); } catch { /* almacenamiento no disponible */ }
  }
}
