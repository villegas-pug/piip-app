import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { PIIP_REPOSITORY } from '../core/piip-repository.token';
import { PiipAuthService } from '../core/piip-auth.service';
import { PiipActivityService } from '../core/piip-activity.service';
import { AuthorizationRecoveryService } from '../core/authorization-recovery.service';

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
  readonly activity = inject(PiipActivityService);
  readonly auth = inject(PiipAuthService);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly authorizationRecovery = inject(AuthorizationRecoveryService);
  readonly currentUrl = signal(this.router.url);
  readonly isHomeRoute = computed(() => this.currentUrl().split(/[?#]/, 1)[0] === '/inicio');
  readonly notificationPending = signal(false);
  readonly unreadNotificationCount = computed(() => this.repository.notifications().filter((item) => !item.read).length);
  readonly hasUnreadNotifications = computed(() => this.unreadNotificationCount() > 0);
  readonly activeExecutingUnit = computed(() =>
    this.repository.executingUnits().find((unit) => unit.id === this.repository.selectedExecutingUnitId()),
  );
  readonly effectiveRoleLabel = computed(() => this.repository.role() ?? 'Sin rol en esta Unidad Ejecutora');
  readonly canOpenUserAdministration = computed(() =>
    this.repository.canAdministerExecutingUnit(this.repository.selectedExecutingUnitId()),
  );
  readonly userAdministrationUnits = computed(() =>
    this.repository.executingUnits().filter((unit) => this.repository.canAdministerExecutingUnit(unit.id)),
  );
  readonly userAdministrationAvailability = computed(() =>
    this.userAdministrationUnits().map((unit) => unit.code).join(', '),
  );

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
    this.authorizationRecovery.setRetryHandler(() => Promise.resolve(this.repository.initialize()));
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.currentUrl.set(event.urlAfterRedirects));
  }

  isNavigationActive(item: NavigationItem): boolean {
    return isNavigationRouteActive(item.route, this.currentUrl());
  }

  async showNotifications(): Promise<void> {
    if (this.notificationPending()) return;
    this.notificationPending.set(true);
    try {
      await this.router.navigateByUrl('/inicio');
      window.setTimeout(() => {
        const target = document.getElementById('mis-notificaciones');
        target?.focus();
        target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 0);
    } finally {
      this.notificationPending.set(false);
    }
  }

  async retryAuthorizationRecovery(): Promise<void> {
    await this.authorizationRecovery.retry();
  }

  toggleRole(): void {
    this.repository.toggleRole();
    if (this.repository.role() === 'Consulta externa' && this.currentUrl().startsWith('/auditoria')) {
      void this.router.navigateByUrl('/inicio');
    }
    this.snackBar.open(`Perfil activo: ${this.repository.role()}.`, 'Cerrar', { duration: 2600 });
  }

  async selectExecutingUnit(executingUnitId: number): Promise<void> {
    if (this.activity.isBlocking() || executingUnitId === this.repository.selectedExecutingUnitId()) return;
    const routeBeforeSelection = this.currentUrl();
    try {
      await this.activity.runBlocking(
        'Actualizando información de la Unidad Ejecutora...',
        () => Promise.resolve(this.repository.selectExecutingUnit(executingUnitId)),
      );
      const lostAdministratorScope = !this.repository.canAdministerExecutingUnit(executingUnitId);
      const routePath = routeBeforeSelection.split(/[?#]/, 1)[0];
      if (lostAdministratorScope && routePath === '/administracion/usuarios') {
        this.snackBar.open(
          'Saliste de Administración de usuarios porque la UE activa no tiene rol Administrador PIIP.',
          'Cerrar',
          { duration: 5200 },
        );
      }
      if (lostAdministratorScope && isActiveScopeAdministratorRoute(routeBeforeSelection)) {
        await this.router.navigateByUrl('/inicio');
      }
    } catch (error) {
      this.snackBar.open(error instanceof Error ? error.message : 'No fue posible cambiar de Unidad Ejecutora.', 'Cerrar', { duration: 3800 });
    }
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }

  async openUserAdministration(): Promise<void> {
    if (!this.canOpenUserAdministration()) return;
    await this.router.navigateByUrl('/administracion/usuarios');
  }

  async retryConnection(): Promise<void> {
    this.repository.clearError();
    await this.activity.runBlocking(
      'Reconectando con PIIP...',
      () => Promise.resolve(this.repository.initialize()),
    );
  }
}

function isActiveScopeAdministratorRoute(url: string): boolean {
  const path = url.split(/[?#]/, 1)[0];
  return path === '/iniciativas/nueva'
    || path === '/proyectos/nuevo/preexistente'
    || path.startsWith('/proyectos/nuevo/derivado/')
    || path === '/administracion/usuarios';
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
