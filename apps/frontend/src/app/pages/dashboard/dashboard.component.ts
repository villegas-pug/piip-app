import { ChangeDetectionStrategy, Component, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { HomePortfolioQuery, PiipRecordType, PiipStatus } from '../../core/piip.models';
import { INITIATIVE_STATUSES, PIIP_CATALOGS, PROJECT_STATUSES } from '../../core/piip.catalogs';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, MatIconModule, RouterLink, PiipPaginationComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnDestroy {
  readonly repository = inject(PIIP_REPOSITORY);
  readonly pageSize = 5;
  readonly query = signal<HomePortfolioQuery>({ executingUnitId: 0, q: '', type: 'Todos', status: 'Todos', page: 0, size: this.pageSize });
  readonly notificationTab = signal<'all' | 'unread'>('all');
  readonly notificationsExpanded = signal(false);
  readonly activeUnit = computed(() => this.repository.executingUnits().find((unit) => unit.id === this.repository.selectedExecutingUnitId()));
  readonly statusOptions = computed<readonly PiipStatus[]>(() => {
    const type = this.query().type;
    if (type === 'Iniciativa') return INITIATIVE_STATUSES;
    if (type === 'Proyecto') return PROJECT_STATUSES;
    return PIIP_CATALOGS.statuses;
  });
  readonly allNotifications = computed(() => [...this.repository.notifications()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
  readonly unreadNotifications = computed(() => this.allNotifications().filter((item) => !item.read));
  readonly visibleNotifications = computed(() => {
    const values = this.notificationTab() === 'unread' ? this.unreadNotifications() : this.allNotifications();
    return this.notificationsExpanded() ? values : values.slice(0, 3);
  });
  readonly statusCounts = computed(() => this.repository.homePortfolio().statusCounts);
  readonly maximumStatusCount = computed(() => Math.max(...this.statusCounts().map((item) => item.count), 1));
  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect(() => {
      const id = this.repository.selectedExecutingUnitId();
      if (id === null) return;
      untracked(() => {
        this.query.update((value) => ({ ...value, executingUnitId: id, page: 0 }));
        void this.loadPortfolio();
      });
    });
  }

  onSearch(value: string): void {
    this.query.update((current) => ({ ...current, q: value, page: 0 }));
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => { void this.loadPortfolio(); }, 300);
  }

  changeType(value: string): void {
    const type = value as PiipRecordType | 'Todos';
    const currentStatus = this.query().status;
    const valid = currentStatus === 'Todos' || (type === 'Todos'
      ? PIIP_CATALOGS.statuses.includes(currentStatus)
      : type === 'Iniciativa' ? INITIATIVE_STATUSES.includes(currentStatus as typeof INITIATIVE_STATUSES[number])
        : PROJECT_STATUSES.includes(currentStatus as typeof PROJECT_STATUSES[number]));
    this.query.update((current) => ({ ...current, type, status: valid ? current.status : 'Todos', page: 0 }));
    void this.loadPortfolio();
  }

  changeStatus(value: string): void { this.query.update((current) => ({ ...current, status: value as PiipStatus | 'Todos', page: 0 })); void this.loadPortfolio(); }
  changePage(page: number): void { this.query.update((current) => ({ ...current, page })); void this.loadPortfolio(); }
  resetFilters(): void { this.query.update((current) => ({ ...current, q: '', type: 'Todos', status: 'Todos', page: 0 })); void this.loadPortfolio(); }
  toggleNotifications(): void { this.notificationsExpanded.update((expanded) => !expanded); }
  setNotificationTab(tab: 'all' | 'unread'): void { this.notificationTab.set(tab); this.notificationsExpanded.set(false); }

  async markAsRead(id: number): Promise<void> {
    try { await Promise.resolve(this.repository.markNotificationRead(id)); }
    catch (error) { this.repository.notificationsError.set(error instanceof Error ? error.message : 'No fue posible marcar la notificación.'); }
  }

  retryNotifications(): void { void Promise.resolve(this.repository.refreshNotifications()); }
  retryPortfolio(): void { void this.loadPortfolio(); }
  statusCount(status: PiipStatus): number { return this.statusCounts().find((item) => item.status === status)?.count ?? 0; }
  barWidth(value: number): number { return value ? Math.max(8, Math.round((value / this.maximumStatusCount()) * 100)) : 0; }
  detailRoute(item: { recordType: PiipRecordType; code: string }): string[] { return [item.recordType === 'Iniciativa' ? '/iniciativas' : '/proyectos', item.code]; }

  private async loadPortfolio(): Promise<void> {
    const executingUnitId = this.repository.selectedExecutingUnitId();
    if (executingUnitId === null) return;
    await Promise.resolve(this.repository.loadHomePortfolio({ ...this.query(), executingUnitId }));
  }

  ngOnDestroy(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
  }
}
