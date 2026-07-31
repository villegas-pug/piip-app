import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterOutlet } from '@angular/router';
import { PiipActivityService } from './core/piip-activity.service';
import { PIIP_REPOSITORY } from './core/piip-repository.token';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatProgressBarModule, MatProgressSpinnerModule],
  template: `
    @if (activity.isBusy()) {
      <div class="global-progress" role="status" aria-live="polite" aria-label="Cargando contenido">
        <mat-progress-bar mode="indeterminate" />
      </div>
    }
    <router-outlet />
    @if (repository.loading() || activity.isBlocking()) {
      <div class="loading-backdrop" role="status" aria-live="polite">
        <section class="loading-card">
          <mat-spinner diameter="48" />
          <strong>{{ repository.loading() ? 'Cargando PIIP...' : activity.blockingMessage() }}</strong>
          <span>Espera un momento, estamos preparando la información.</span>
        </section>
      </div>
    }
  `,
  styles: `
    :host { display: block; min-height: 100%; }
    .global-progress { position: fixed; inset: 0 0 auto; z-index: 220; height: 4px; }
    .global-progress mat-progress-bar { --mat-progress-bar-active-indicator-color: var(--piip-gold); --mat-progress-bar-track-color: rgba(255,255,255,.45); }
    .loading-backdrop { position: fixed; inset: 0; z-index: 210; display: grid; place-items: center; padding: 20px; background: rgba(0,38,25,.55); backdrop-filter: blur(3px); }
    .loading-card { display: grid; justify-items: center; gap: 12px; width: min(390px, 100%); padding: 30px 26px; border: 1px solid rgba(255,255,255,.7); border-radius: 10px; color: var(--piip-ink); background: #fff; box-shadow: 0 24px 70px rgba(0,34,22,.3); text-align: center; }
    .loading-card mat-spinner { --mat-progress-spinner-active-indicator-color: var(--piip-green-700); }
    .loading-card strong { color: var(--piip-green-950); font-family: var(--piip-display); font-size: 22px; }
    .loading-card span { color: var(--piip-gray-500); }
  `,
  host: {
    '[attr.aria-busy]': 'repository.loading() || activity.isBlocking() || activity.isBusy()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly activity = inject(PiipActivityService);
  private finishNavigation?: () => void;

  constructor() {
    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.finishNavigation?.();
        this.finishNavigation = this.activity.beginNavigation();
      } else if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError) {
        this.finishNavigation?.();
        this.finishNavigation = undefined;
      }
    });
  }
}
