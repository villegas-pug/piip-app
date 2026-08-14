import { TestBed } from '@angular/core/testing';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeFormComponent } from './initiative-form.component';
import { InitiativeReviewDialogComponent } from './initiative-review-dialog.component';

describe('InitiativeFormComponent', () => {
  const open = vi.fn();
  const block = vi.fn(() => ({}));

  beforeEach(async () => {
    open.mockReset();
    block.mockClear();
    open.mockReturnValue({});
    await TestBed.configureTestingModule({ imports: [InitiativeFormComponent], providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatDialog, useValue: { open } }, { provide: Overlay, useValue: { scrollStrategies: { block } } }] }).compileComponents();
  });

  it('starts invalid and uses Presentado only as the official submission state', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    expect(fixture.componentInstance.form.controls.status.value).toBe('Presentado');
    expect(fixture.componentInstance.form.controls.code.value).toBe('Se asignará al registrar');
    expect(fixture.nativeElement.textContent).not.toContain('I-025-2026');
    expect(fixture.nativeElement.textContent).toContain('El borrador es solo una condición local de la UI');
  });

  it('prevents duplicate initiative registrations while one is pending', async () => {
    const repository = TestBed.inject(PIIP_REPOSITORY);
    let releaseRegistration!: (record: ReturnType<typeof repository.portfolioRecords>[number]) => void;
    const pendingRegistration = new Promise<ReturnType<typeof repository.portfolioRecords>[number]>((resolve) => {
      releaseRegistration = resolve;
    });
    const registerInitiative = vi.spyOn(repository, 'registerInitiative').mockReturnValue(pendingRegistration);
    const fixture = TestBed.createComponent(InitiativeFormComponent);

    const first = fixture.componentInstance.registerInitiative();
    const duplicate = fixture.componentInstance.registerInitiative();

    expect(registerInitiative).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.submitting()).toBe(true);
    releaseRegistration(repository.portfolioRecords()[0]);
    await Promise.all([first, duplicate]);
    expect(fixture.componentInstance.submitting()).toBe(false);
  });

  it('opens the final review in a global CDK dialog only when the form is valid', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    const component = fixture.componentInstance;
    component.form.patchValue({
      startDate: '2026-08-14', name: 'Iniciativa', solutionType: 'Solución por definir', source: 'Interna',
      digitalComponent: 'Si', description: 'Descripción', responsible: 'Responsable', responsibleUnits: 'OGTI',
    });
    component.uploadedFilename.set('ficha.pdf');

    component.openReview();

    expect(open).toHaveBeenCalledWith(InitiativeReviewDialogComponent, expect.objectContaining({
      autoFocus: 'first-heading', panelClass: 'initiative-review-dialog-panel', backdropClass: 'initiative-review-dialog-backdrop',
    }));
    expect(block).toHaveBeenCalledOnce();
  });

  it('renders accessible inline errors for the required initiative fields', () => {
    const fixture = TestBed.createComponent(InitiativeFormComponent);
    fixture.componentInstance.form.markAllAsTouched();
    fixture.detectChanges();

    for (const errorId of ['initiative-name-error', 'initiative-description-error', 'initiative-responsible-error', 'initiative-responsible-unit-error', 'initiative-file-error']) {
      expect(fixture.nativeElement.querySelector(`#${errorId}`)?.getAttribute('role')).toBe('alert');
    }
    expect(fixture.nativeElement.querySelector('[formControlName="name"]')?.getAttribute('aria-describedby')).toBe('initiative-name-error');
    expect(fixture.nativeElement.querySelector('[formControlName="description"]')?.getAttribute('aria-describedby')).toBe('initiative-description-error');
  });
});
