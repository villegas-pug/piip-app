import { Injectable, WritableSignal, computed, signal } from '@angular/core';

interface BlockingOperation {
  id: symbol;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class PiipActivityService {
  private readonly activeRequests = signal(0);
  private readonly activeNavigations = signal(0);
  private readonly blockingOperations = signal<BlockingOperation[]>([]);
  private readonly delayedBusy = signal(false);
  private busyDelay?: ReturnType<typeof setTimeout>;

  readonly isBusy = this.delayedBusy.asReadonly();
  readonly isBlocking = computed(() => this.blockingOperations().length > 0);
  readonly blockingMessage = computed(() =>
    this.blockingOperations().at(-1)?.message ?? 'Procesando solicitud...',
  );

  beginRequest(): () => void {
    return this.increment(this.activeRequests);
  }

  beginNavigation(): () => void {
    return this.increment(this.activeNavigations);
  }

  async runBlocking<T>(message: string, operation: () => T | Promise<T>): Promise<T> {
    const blockingOperation: BlockingOperation = { id: Symbol(message), message };
    this.blockingOperations.update((operations) => [...operations, blockingOperation]);
    try {
      return await operation();
    } finally {
      this.blockingOperations.update((operations) =>
        operations.filter((current) => current.id !== blockingOperation.id),
      );
    }
  }

  private increment(counter: WritableSignal<number>): () => void {
    counter.update((count) => count + 1);
    this.synchronizeBusyState();
    let completed = false;
    return () => {
      if (completed) return;
      completed = true;
      counter.update((count) => Math.max(0, count - 1));
      this.synchronizeBusyState();
    };
  }

  private synchronizeBusyState(): void {
    const hasActivity = this.activeRequests() > 0 || this.activeNavigations() > 0;
    if (!hasActivity) {
      if (this.busyDelay) clearTimeout(this.busyDelay);
      this.busyDelay = undefined;
      this.delayedBusy.set(false);
      return;
    }
    if (this.delayedBusy() || this.busyDelay) return;
    this.busyDelay = setTimeout(() => {
      this.busyDelay = undefined;
      if (this.activeRequests() > 0 || this.activeNavigations() > 0) this.delayedBusy.set(true);
    }, 120);
  }
}
