import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import {
  ProjectRegistrationDialogComponent,
  ProjectRegistrationDialogResult,
} from './project-registration-dialog.component';

describe('ProjectRegistrationDialogComponent', () => {
  const close = vi.fn<(result?: ProjectRegistrationDialogResult) => void>();

  beforeEach(async () => {
    close.mockReset();
    await TestBed.configureTestingModule({
      imports: [ProjectRegistrationDialogComponent],
      providers: [
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MAT_DIALOG_DATA, useValue: { initialView: 'initiative-selection' } },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
  });

  it('shows only eligible initiatives and searches all visible metadata', () => {
    const fixture = TestBed.createComponent(ProjectRegistrationDialogComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.eligibleInitiatives().map((initiative) => initiative.code)).toEqual(['I-019-2026']);
    expect(fixture.nativeElement.textContent).toContain('Fortalecimiento de capacidades para la gestión de la innovación agraria');
    expect(fixture.nativeElement.textContent).not.toContain('I-019-2026');

    for (const searchTerm of ['I-019', 'Fortalecimiento', 'DIPNA', 'Carlos Rojas']) {
      component.searchControl.setValue(searchTerm);
      expect(component.filteredInitiatives().map((initiative) => initiative.code)).toEqual(['I-019-2026']);
    }

    component.searchControl.setValue('sin coincidencias');
    expect(component.filteredInitiatives()).toEqual([]);
  });

  it('requires an explicit selection before returning a derived-project result', () => {
    const fixture = TestBed.createComponent(ProjectRegistrationDialogComponent);
    const component = fixture.componentInstance;

    component.continueWithInitiative();
    expect(close).not.toHaveBeenCalled();

    component.selectInitiative('I-019-2026');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Fortalecimiento de capacidades para la gestión de la innovación agraria');
    component.continueWithInitiative();

    expect(close).toHaveBeenCalledWith({ mode: 'DERIVED_FROM_INITIATIVE', initiativeCode: 'I-019-2026' });
  });

  it('returns to type selection and emits the preexisting-project result', () => {
    const fixture = TestBed.createComponent(ProjectRegistrationDialogComponent);
    const component = fixture.componentInstance;

    component.selectInitiative('I-019-2026');
    component.showTypeSelection();

    expect(component.view()).toBe('type-selection');
    expect(component.selectedInitiativeCode()).toBeNull();
    expect(component.searchControl.value).toBe('');

    component.choosePreexistingProject();
    expect(close).toHaveBeenCalledWith({ mode: 'PREEXISTING' });
  });
});
