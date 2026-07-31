import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { AuditEvent } from '../../core/piip.models';
import { AuditEventDetailDialogComponent } from './audit-event-detail-dialog.component';
import { PresentedAuditEvent, presentAuditEvent } from './audit-event.presenter';
import { PiipPaginationComponent } from '../../shared/pagination/piip-pagination.component';
import { clampPageIndex, paginateItems } from '../../shared/pagination/piip-pagination.utils';

@Component({
  selector: 'app-audit',
  imports: [ReactiveFormsModule, MatIconModule, PiipPaginationComponent],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly initialRecord = this.route.snapshot.queryParamMap.get('record') ?? '';
  readonly filters = this.formBuilder.nonNullable.group({ record: this.initialRecord, eventType: 'Todos', user: 'Todos', from: '', to: '' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly pageIndex = signal(0);
  readonly recordCodes = computed(() => [...new Set(this.repository.portfolioRecords().map((record) => record.code))]);
  readonly userOptions = computed(() => [...new Set(this.repository.auditEvents().map((event) => event.user).filter(Boolean))].sort());
  readonly filteredEvents = computed(() => {
    const filters = this.filterValue();
    return this.repository.auditEvents().filter((event) =>
      (!filters.record || event.recordCode === filters.record) &&
      (filters.user === 'Todos' || event.user === filters.user) &&
      (filters.eventType === 'Todos' || this.eventCategory(event.event, Boolean(event.documentName)) === filters.eventType),
    );
  });
  readonly deniedAccesses = computed(() => this.repository.auditAccesses().filter((access) => access.status === 401 || access.status === 403).length);
  readonly failedAccesses = computed(() => this.repository.auditAccesses().filter((access) => access.status >= 500).length);
  readonly presentedEvents = computed(() => this.filteredEvents().map(presentAuditEvent));
  readonly currentPage = computed(() => clampPageIndex(this.pageIndex(), this.presentedEvents().length));
  readonly pagedEvents = computed(() => paginateItems(this.presentedEvents(), this.currentPage()));

  constructor() {
    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.pageIndex.set(0));
  }

  resetFilters(): void {
    this.filters.reset({ record: this.initialRecord, eventType: 'Todos', user: 'Todos', from: '', to: '' });
  }

  showDetail(event: PresentedAuditEvent): void {
    this.dialog.open(AuditEventDetailDialogComponent, {
      data: event,
      maxWidth: 'calc(100vw - 32px)',
      autoFocus: 'first-heading',
    });
  }

  private eventCategory(event: string, hasDocument: boolean): 'Creación' | 'Documento' | 'Transición' {
    if (hasDocument || /cargad/i.test(event)) return 'Documento';
    if (/cread|registrad/i.test(event)) return 'Creación';
    return 'Transición';
  }
}
