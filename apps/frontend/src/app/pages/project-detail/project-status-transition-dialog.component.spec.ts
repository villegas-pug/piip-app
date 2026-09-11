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
        providers: [PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialogRef, useValue: dialogRef }, { provide: MAT_DIALOG_DATA, useValue: { projectCode: 'P-005-2026', currentStatus: 'PROJECT_IN_PROGRESS', options: ['PRODUCT_APPROVED', 'PRODUCT_NOT_APPROVED', 'SUSPENDED', 'CANCELLED'] } }],
    }).compileComponents();
  });

  it('solo permite seleccionar destinos recibidos por la matriz contextual', () => {
    const fixture = TestBed.createComponent(ProjectStatusTransitionDialogComponent);
    const component = fixture.componentInstance;

    component.selectTarget('PRODUCT_APPROVED');
    expect(component.selectedTarget()).toBe('PRODUCT_APPROVED');

    component.selectTarget('FINISHED');
    expect(component.selectedTarget()).toBe('PRODUCT_APPROVED');
  });

  it('confirma la transición con la observación y cierra el diálogo', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const transition = vi.spyOn(repository, 'transitionProjectStatus').mockResolvedValue(repository.portfolioRecords()[0]);
    const fixture = TestBed.createComponent(ProjectStatusTransitionDialogComponent);
    const component = fixture.componentInstance;
    component.selectTarget('PRODUCT_APPROVED');
    component.transitionForm.patchValue({ observation: 'Validación aprobada' });

    await component.confirm();

    expect(transition).toHaveBeenCalledWith({ projectCode: 'P-005-2026', targetStatus: 'PRODUCT_APPROVED', observation: 'Validación aprobada' });
    expect(close).toHaveBeenCalledWith({ targetStatus: 'PRODUCT_APPROVED' });
    expect(dialogRef.disableClose).toBe(false);
  });
});
