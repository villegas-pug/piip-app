import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { ProjectStatusTransitionDialogComponent } from './project-status-transition-dialog.component';

describe('ProjectStatusTransitionDialogComponent', () => {
  const close = vi.fn();
  const dialogRef = { close, disableClose: false };

  beforeEach(async () => {
    close.mockReset();
    dialogRef.disableClose = false;
    await TestBed.configureTestingModule({
      imports: [ProjectStatusTransitionDialogComponent],
      providers: [PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialogRef, useValue: dialogRef }, { provide: MAT_DIALOG_DATA, useValue: { projectCode: 'P-005-2026', currentStatus: 'Proyecto en ejecución', options: ['Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado'] } }],
    }).compileComponents();
  });

  it('solo permite seleccionar destinos recibidos por la matriz contextual', () => {
    const fixture = TestBed.createComponent(ProjectStatusTransitionDialogComponent);
    const component = fixture.componentInstance;

    component.selectTarget('Producto aprobado');
    expect(component.selectedTarget()).toBe('Producto aprobado');

    component.selectTarget('Finalizado');
    expect(component.selectedTarget()).toBe('Producto aprobado');
  });

  it('confirma la transición con la observación y cierra el diálogo', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const transition = vi.spyOn(repository, 'transitionProjectStatus').mockResolvedValue(repository.portfolioRecords()[0]);
    const fixture = TestBed.createComponent(ProjectStatusTransitionDialogComponent);
    const component = fixture.componentInstance;
    component.selectTarget('Producto aprobado');
    component.transitionForm.patchValue({ observation: 'Validación aprobada' });

    await component.confirm();

    expect(transition).toHaveBeenCalledWith({ projectCode: 'P-005-2026', targetStatus: 'Producto aprobado', observation: 'Validación aprobada' });
    expect(close).toHaveBeenCalledWith({ targetStatus: 'Producto aprobado' });
    expect(dialogRef.disableClose).toBe(false);
  });
});
