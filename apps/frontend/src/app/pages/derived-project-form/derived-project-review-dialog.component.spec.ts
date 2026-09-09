import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DerivedProjectReviewDialogComponent, DerivedProjectReviewDialogData } from './derived-project-review-dialog.component';

describe('DerivedProjectReviewDialogComponent', () => {
  const close = vi.fn();
  const registerProject = vi.fn<() => Promise<boolean>>();
  const dialogRef = { close, disableClose: false };
  const data: DerivedProjectReviewDialogData = {
    initiativeCode: 'I-007-2026',
    projectCode: 'P-005-2026',
    name: 'Agro exportación',
    startDateIso: '2026-08-23',
    startDate: '23 de agosto de 2026',
    solutionType: 'Solución por definir',
    source: 'Innovación abierta',
    digitalComponent: 'No',
    responsible: 'Responsable PIIP',
    organizationalUnits: [
      { id: 1, code: 'UO-1', name: 'Oficina de Planeamiento y Modernización', acronym: 'OPM', parentId: null, executingUnitId: 1, active: true },
      { id: 2, code: 'UO-2', name: 'Oficina de Innovación', acronym: 'OIN', parentId: null, executingUnitId: 1, active: true },
    ],
    description: 'Descripción del proyecto derivado.',
    keyResults: '',
    registerProject,
  };

  beforeEach(async () => {
    close.mockReset();
    registerProject.mockReset();
    dialogRef.disableClose = false;
    await TestBed.configureTestingModule({
      imports: [DerivedProjectReviewDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();
  });

  it('presenta la trazabilidad y el resumen crítico con etiquetas legibles', () => {
    const fixture = TestBed.createComponent(DerivedProjectReviewDialogComponent);
    fixture.detectChanges();
    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(content).toContain('I-007-2026');
    expect(content).toContain('P-005-2026');
    expect(content).toContain('Solución por definir');
    expect(content).toContain('Unidades Orgánicas Involucradas');
    const units = Array.from<Element>(fixture.nativeElement.querySelectorAll('.ou-summary-item'));
    expect(units.map((item) => ({
      number: item.querySelector('.ou-summary-number')?.getAttribute('aria-label'),
      description: item.querySelector('.ou-summary-description strong')?.textContent?.trim(),
      acronym: item.querySelector('.ou-summary-acronym > span:last-child')?.textContent?.trim(),
    }))).toEqual([
      { number: 'Nro 1', description: 'Oficina de Planeamiento y Modernización', acronym: 'OPM' },
      { number: 'Nro 2', description: 'Oficina de Innovación', acronym: 'OIN' },
    ]);
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(content).toContain('Sin información registrada');
    expect(content).toContain('Los documentos no se duplicarán');
    expect(fixture.nativeElement.querySelector('time')?.getAttribute('datetime')).toBe('2026-08-23');
    expect(fixture.nativeElement.querySelector('#derived-project-review-title')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#derived-project-review-description')).not.toBeNull();
  });

  it('bloquea cierres y envíos duplicados mientras registra', async () => {
    let resolveRegistration!: (result: boolean) => void;
    registerProject.mockReturnValue(new Promise<boolean>((resolve) => resolveRegistration = resolve));
    const fixture = TestBed.createComponent(DerivedProjectReviewDialogComponent);
    const component = fixture.componentInstance;

    const first = component.register();
    const duplicate = component.register();
    expect(registerProject).toHaveBeenCalledOnce();
    expect(component.submitting()).toBe(true);
    expect(dialogRef.disableClose).toBe(true);

    resolveRegistration(true);
    await Promise.all([first, duplicate]);
    expect(close).toHaveBeenCalledOnce();
    expect(component.submitting()).toBe(false);
    expect(dialogRef.disableClose).toBe(false);
  });

  it('permanece abierto y reactiva las acciones cuando el registro falla', async () => {
    registerProject.mockResolvedValue(false);
    const fixture = TestBed.createComponent(DerivedProjectReviewDialogComponent);

    await fixture.componentInstance.register();

    expect(close).not.toHaveBeenCalled();
    expect(fixture.componentInstance.submitting()).toBe(false);
    expect(dialogRef.disableClose).toBe(false);
  });
});
