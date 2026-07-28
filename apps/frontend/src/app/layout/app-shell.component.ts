import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { PIIP_REPOSITORY } from '../core/piip-repository.token';
import { PiipAuthService } from '../core/piip-auth.service';

interface NavigationItem {
  label: string;
  icon: string;
  route: string;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, MatIconModule, MatMenuModule, MatSnackBarModule],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly snackBar = inject(MatSnackBar);
  readonly auth = inject(PiipAuthService);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly currentUrl = signal(this.router.url);
  readonly hasUnreadNotifications = computed(() => this.repository.notifications().some((item) => !item.read));

  readonly navigation: NavigationItem[] = [
    { label: 'Inicio', icon: 'home', route: '/inicio' },
    { label: 'Iniciativas', icon: 'folder_open', route: '/iniciativas' },
    { label: 'Proyectos', icon: 'business_center', route: '/proyectos' },
    { label: 'Documentos', icon: 'description', route: '/documentos' },
    { label: 'Auditoría', icon: 'shield', route: '/auditoria', adminOnly: true },
  ];

  readonly pageTitle = computed(() => {
    const url = this.currentUrl();
    if (url.includes('/iniciativas/nueva')) return 'Nueva iniciativa';
    if (url.includes('/proyectos/nuevo/derivado/')) return 'Registrar proyecto derivado';
    if (url.includes('/proyectos/nuevo/preexistente')) return 'Registrar proyecto preexistente';
    if (url.includes('/documentos')) return 'Documentos';
    if (url.startsWith('/iniciativas')) return 'Iniciativas';
    if (url.startsWith('/proyectos')) return 'Proyectos';
    if (url.startsWith('/auditoria')) return 'Auditoría de expedientes';
    if (url.startsWith('/administracion')) return 'Administración de usuarios';
    return 'Gestión de Iniciativas y Proyectos';
  });

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.currentUrl.set(event.urlAfterRedirects));
  }

  isNavigationActive(item: NavigationItem): boolean {
    return isNavigationRouteActive(item.route, this.currentUrl());
  }

  async showNotifications(): Promise<void> {
    const unread = this.repository.notifications().filter((item) => !item.read);
    const latest = unread[0] ?? this.repository.notifications()[0];
    this.snackBar.open(latest?.message ?? 'No tienes notificaciones pendientes.', 'Cerrar', { duration: 4200 });
    if (latest && !latest.read) await Promise.resolve(this.repository.markNotificationRead(latest.id));
  }

  toggleRole(): void {
    this.repository.toggleRole();
    if (this.repository.role() === 'Consulta externa' && this.currentUrl().startsWith('/auditoria')) {
      void this.router.navigateByUrl('/inicio');
    }
    this.snackBar.open(`Perfil activo: ${this.repository.role()}.`, 'Cerrar', { duration: 2600 });
  }

  async selectExecutingUnit(event: Event): Promise<void> {
    const executingUnitId = Number((event.target as HTMLSelectElement).value);
    try {
      await Promise.resolve(this.repository.selectExecutingUnit(executingUnitId));
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cambiar de Unidad Ejecutora.', 'Cerrar', { duration: 3800 });
    }
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }

  async retryConnection(): Promise<void> {
    this.repository.clearError();
    await Promise.resolve(this.repository.initialize());
  }
}

export function isNavigationRouteActive(navigationRoute: string, currentUrl: string): boolean {
  const url = currentUrl.split(/[?#]/, 1)[0];
  const isDocumentsContext =
    url === '/documentos' || /^\/(iniciativas|proyectos)\/[^/]+\/documentos$/.test(url);

  if (navigationRoute === '/documentos') return isDocumentsContext;
  if (isDocumentsContext) return false;
  if (navigationRoute === '/inicio') return url === navigationRoute;
  return url === navigationRoute || url.startsWith(`${navigationRoute}/`);
}
