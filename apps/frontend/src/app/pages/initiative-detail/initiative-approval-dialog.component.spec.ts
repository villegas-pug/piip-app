import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeApprovalDialogComponent } from './initiative-approval-dialog.component';

describe('InitiativeApprovalDialogComponent', () => {
  const close = vi.fn();
  const dialogRef = { close, disableClose: false };

  beforeEach(async () => {
    close.mockReset();
    dialogRef.disableClose = false;
    await TestBed.configureTestingModule({
      imports: [InitiativeApprovalDialogComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            initiativeCode: 'I-024-2026',
            initiativeName: 'Iniciativa de prueba',
            currentStatus: 'Presentado',
            approvalDocuments: [
              { name: 'Informe técnico', required: false, filename: null, version: null, uploadedAt: null, state: 'Pendiente' },
              { name: 'Decisión formal', required: false, filename: 'decision.pdf', version: '1', uploadedAt: '20/05/2026', state: 'Cargado' },
            ],
          },
        },
      ],
    }).compileComponents();
  });

  it('muestra la advertencia documental sin bloquear la aprobación', () => {
    const fixture = TestBed.createComponent(InitiativeApprovalDialogComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.textContent).toContain('Documentación pendiente');
    expect(host.textContent).toContain('Informe técnico');
    expect(host.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(false);
    expect(host.querySelector('.status-tag mat-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('registra la aprobación y presenta el estado de éxito dentro del diálogo', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const approve = vi.spyOn(repository, 'approveInitiative');
    const fixture = TestBed.createComponent(InitiativeApprovalDialogComponent);
    const component = fixture.componentInstance;
    component.approvalForm.patchValue({ observation: 'Revisión conforme' });

    await component.approve();
    fixture.detectChanges();

    expect(approve).toHaveBeenCalledWith({
      initiativeCode: 'I-024-2026',
      targetStatus: 'Iniciativa aprobada',
      observation: 'Revisión conforme',
    });
    expect(component.approved()).toBe(true);
    expect(dialogRef.disableClose).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Crear proyecto ahora');
  });

  it('mantiene el diálogo abierto y muestra el error de la operación', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    vi.spyOn(repository, 'approveInitiative').mockRejectedValue(new Error('Conflicto de versión'));
    const fixture = TestBed.createComponent(InitiativeApprovalDialogComponent);

    await fixture.componentInstance.approve();
    fixture.detectChanges();

    expect(fixture.componentInstance.approved()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain('Conflicto de versión');
    expect(close).not.toHaveBeenCalled();
  });
});
