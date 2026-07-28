import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { AuditEvent } from '../../core/piip.models';

@Component({
  selector: 'app-audit',
  imports: [ReactiveFormsModule, MatIconModule],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  readonly repository = inject(PIIP_REPOSITORY);
  readonly initialRecord = this.route.snapshot.queryParamMap.get('record') ?? '';
  readonly filters = this.formBuilder.nonNullable.group({ record: this.initialRecord, eventType: 'Todos', user: 'Todos', from: '', to: '' });
  private readonly filterValue = toSignal(this.filters.valueChanges, { initialValue: this.filters.getRawValue() });
  readonly recordCodes = computed(() => [...new Set(this.repository.portfolioRecords().map((record) => record.code))]);
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

  resetFilters(): void {
    this.filters.reset({ record: this.initialRecord, eventType: 'Todos', user: 'Todos', from: '', to: '' });
  }

  showDetail(event: AuditEvent): void {
    this.snackBar.open(event.observation || event.event, 'Cerrar', { duration: 5000 });
  }

  private eventCategory(event: string, hasDocument: boolean): 'Creación' | 'Documento' | 'Transición' {
    if (hasDocument || /cargad/i.test(event)) return 'Documento';
    if (/cread|registrad/i.test(event)) return 'Creación';
    return 'Transición';
  }
}
