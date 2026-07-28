import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, MatIconModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly snackBar = inject(MatSnackBar);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly alerts = computed(() => this.repository.workItems().filter((item) => item.alert === 'VENCIDA' || item.alert === 'PROXIMA'));
  readonly unreadNotifications = computed(() => this.repository.notifications().filter((item) => !item.read));

  showMessage(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: 2800 });
  }

  statusCount(status: string): number {
    return this.repository.dashboardSummary().portfolioByStatus[status] ?? 0;
  }

  barWidth(value: number): number {
    const values = Object.values(this.repository.dashboardSummary().portfolioByStatus);
    const maximum = Math.max(...values, 1);
    return Math.max(value ? 8 : 0, Math.round((value / maximum) * 100));
  }
}
