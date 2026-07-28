import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { AuditComponent } from './audit.component';

describe('AuditComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
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
});
