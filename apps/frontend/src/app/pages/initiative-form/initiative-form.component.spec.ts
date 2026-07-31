import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeFormComponent } from './initiative-form.component';

describe('InitiativeFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InitiativeFormComponent], providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }] }).compileComponents();
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
});
