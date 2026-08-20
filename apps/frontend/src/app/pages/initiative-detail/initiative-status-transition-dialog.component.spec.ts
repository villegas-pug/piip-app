import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeStatusTransitionDialogComponent } from './initiative-status-transition-dialog.component';

describe('InitiativeStatusTransitionDialogComponent', () => {
  const close = vi.fn();
  const dialogRef = { close, disableClose: false };

  beforeEach(async () => {
    close.mockReset();
    dialogRef.disableClose = false;
    await TestBed.configureTestingModule({
      imports: [InitiativeStatusTransitionDialogComponent],
      providers: [
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            initiativeCode: 'I-024-2026',
            currentStatus: 'Presentado',
            options: ['Iniciativa archivada', 'No Admisible'],
          },
        },
      ],
    }).compileComponents();
  });

  it('limita la selección a los destinos recibidos y autorizados por la matriz', () => {
    const fixture = TestBed.createComponent(InitiativeStatusTransitionDialogComponent);
    const component = fixture.componentInstance;

    component.selectTarget('Iniciativa archivada');
    expect(component.selectedTarget()).toBe('Iniciativa archivada');

    component.selectTarget('Producto aprobado' as never);
    expect(component.selectedTarget()).toBe('Iniciativa archivada');
  });

  it('confirma la transición con la observación y cierra con resultado', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const transition = vi.spyOn(repository, 'transitionInitiativeStatus');
    const fixture = TestBed.createComponent(InitiativeStatusTransitionDialogComponent);
    const component = fixture.componentInstance;
    component.selectTarget('No Admisible');
    component.transitionForm.patchValue({ observation: 'No cumple criterios' });

    await component.confirm();

    expect(transition).toHaveBeenCalledWith({
      initiativeCode: 'I-024-2026',
      targetStatus: 'No Admisible',
      observation: 'No cumple criterios',
    });
    expect(close).toHaveBeenCalledWith({ targetStatus: 'No Admisible' });
    expect(dialogRef.disableClose).toBe(false);
  });

  it('muestra el error dentro del diálogo y no lo cierra', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    vi.spyOn(repository, 'transitionInitiativeStatus').mockRejectedValue(new Error('Transición rechazada'));
    const fixture = TestBed.createComponent(InitiativeStatusTransitionDialogComponent);
    fixture.componentInstance.selectTarget('Iniciativa archivada');

    await fixture.componentInstance.confirm();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain('Transición rechazada');
    expect(close).not.toHaveBeenCalled();
  });
});
