import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { AuditEventDetailDialogComponent } from './audit-event-detail-dialog.component';
import { AuditComponent } from './audit.component';
import { presentAuditEvent } from './audit-event.presenter';

describe('AuditComponent', () => {
  const open = vi.fn();

  beforeEach(async () => {
    open.mockReset();
    await TestBed.configureTestingModule({
      imports: [AuditComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open } }],
    }).compileComponents();
  });

  it('lists only portfolio codes in the dossier filter', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.auditEvents.update((events) => [{
      recordCode: '2', timestamp: '28/07/2026', event: 'TAREA_CREADA', user: 'Administrador PIIP', email: '',
      observation: '{"registro":"I-024-2026"}', icon: 'history',
    }, ...events]);
    const fixture = TestBed.createComponent(AuditComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.recordCodes()).not.toContain('2');
    expect(fixture.componentInstance.recordCodes()).toEqual(expect.arrayContaining(['I-024-2026', 'P-005-2026']));
    expect(fixture.nativeElement.querySelector('select[formControlName="record"] option').textContent).toBe('Todos');
  });

  it('uses readable users for the filter and opens the technical detail dialog', () => {
    const fixture = TestBed.createComponent(AuditComponent);
    fixture.detectChanges();
    const event = presentAuditEvent(fixture.componentInstance.filteredEvents()[0]);

    expect(fixture.componentInstance.userOptions()).toContain('Administrador PIIP');
    fixture.componentInstance.showDetail(event);

    expect(open).toHaveBeenCalledWith(AuditEventDetailDialogComponent, expect.objectContaining({ data: event, autoFocus: 'first-heading' }));
  });

  it('paginates the same five events used by desktop rows and mobile cards', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.auditEvents()[0];
    repository.auditEvents.set(Array.from({ length: 6 }, (_, index) => ({ ...template, timestamp: `31/07/2026\\n10:0${index}`, event: `EVENTO_${index}` })));
    const fixture = TestBed.createComponent(AuditComponent);
    const component = fixture.componentInstance;

    expect(component.pagedEvents()).toHaveLength(5);
    component.pageIndex.set(1);
    expect(component.pagedEvents()).toHaveLength(1);

    component.filters.patchValue({ eventType: 'Creación' });
    expect(component.currentPage()).toBe(0);
  });

  it('presenta el motivo seguro nullable de los accesos rechazados', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.auditAccesses.set([
      { correlationId: 'corr-1', occurredAt: '2026-08-23T12:00:00Z', subject: 'subject', roles: '', method: 'GET', path: '/admin/users', status: 403, durationMs: 12, safeReason: 'FORBIDDEN_SCOPE' },
      { correlationId: 'corr-2', occurredAt: '2026-08-23T12:01:00Z', subject: 'subject', roles: '', method: 'GET', path: '/admin/users', status: 403, durationMs: 10, safeReason: null },
    ]);
    const fixture = TestBed.createComponent(AuditComponent);
    fixture.detectChanges();

    const reasons = Array.from(fixture.nativeElement.querySelectorAll('.access-reasons li')).map((item) => (item as Element).textContent?.trim());
    expect(reasons).toEqual(['info_outline/admin/users: FORBIDDEN_SCOPE', 'info_outline/admin/users: Motivo no disponible']);
  });
});
