import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { normalizeInternalReturnUrl, PiipAuthService } from '../../core/piip-auth.service';

@Component({
  selector: 'app-login',
  imports: [MatIconModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent implements OnInit {
  readonly auth = inject(PiipAuthService);
  readonly redirecting = signal(false);
  readonly loginError = signal<string | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly returnUrl = normalizeInternalReturnUrl(
    this.route.snapshot.queryParamMap.get('returnUrl'),
  );

  ngOnInit(): void {
    if (this.auth.authenticated()) {
      void this.completeAuthentication();
    }
  }

  async login(): Promise<void> {
    if (this.redirecting() || this.auth.configurationError()) return;

    this.redirecting.set(true);
    this.loginError.set(null);
    try {
      await this.auth.login(this.returnUrl);
    } catch {
      this.loginError.set(
        'No fue posible redirigir al acceso institucional. Inténtalo nuevamente.',
      );
      this.redirecting.set(false);
    }
  }

  retry(): void {
    window.location.reload();
  }

  private async completeAuthentication(): Promise<void> {
    const destination = this.auth.consumePostLoginRoute(this.returnUrl);
    await this.router.navigateByUrl(destination, { replaceUrl: true });
  }
}
