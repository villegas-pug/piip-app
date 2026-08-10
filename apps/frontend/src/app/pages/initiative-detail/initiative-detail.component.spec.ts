import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativeDetailComponent } from './initiative-detail.component';

describe('InitiativeDetailComponent', () => {
  let approvalAction = false;
  const paramMap = convertToParamMap({ code: 'I-024-2026' });

  beforeEach(async () => {
    approvalAction = false;
    await TestBed.configureTestingModule({
      imports: [InitiativeDetailComponent],
      providers: [
        provideRouter([]),
        PiipMockRepository,
        { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: {
          paramMap: of(paramMap),
          snapshot: {
            paramMap,
            get queryParamMap() { return convertToParamMap(approvalAction ? { action: 'approve' } : {}); },
          },
        } },
      ],
    }).compileComponents();
  });

  it('opens the approval dialog from the action button and closes it', () => {
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    const approvalButton = Array.from(nativeElement.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Registrar aprobación'));
    if (!approvalButton) throw new Error('No se encontró el botón de aprobación.');
    approvalButton.click();
    fixture.detectChanges();

    expect(nativeElement.querySelector('[role="dialog"]')).not.toBeNull();

    nativeElement.querySelector<HTMLButtonElement>('[aria-label="Cerrar"]')?.click();
    fixture.detectChanges();

    expect(nativeElement.querySelector('[role="dialog"]')).toBeNull();
  });

  it('opens the approval dialog when action=approve is present in the URL', () => {
    approvalAction = true;
    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
  });

  it('shows the approval submission state until the operation completes', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const approvedRecord = repository.portfolioRecords().find((record) => record.code === 'I-024-2026');
    if (!approvedRecord) throw new Error('No se encontró la iniciativa de prueba.');

    let resolveApproval!: (record: typeof approvedRecord) => void;
    const pendingApproval = new Promise<typeof approvedRecord>((resolve) => { resolveApproval = resolve; });
    vi.spyOn(repository, 'approveInitiative').mockReturnValue(pendingApproval);

    const fixture = TestBed.createComponent(InitiativeDetailComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const approvalButton = Array.from(nativeElement.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Registrar aprobación'));
    if (!approvalButton) throw new Error('No se encontró el botón de aprobación.');
    approvalButton.click();
    fixture.detectChanges();

    const confirmButton = Array.from(nativeElement.querySelectorAll<HTMLButtonElement>('.approval-dialog footer button'))
      .find((button) => button.textContent?.includes('Confirmar aprobación'));
    if (!confirmButton) throw new Error('No se encontró el botón de confirmación.');
    confirmButton.click();
    fixture.detectChanges();

    const approvalDialog = nativeElement.querySelector<HTMLElement>('.approval-dialog');
    expect(approvalDialog?.getAttribute('aria-busy')).toBe('true');
    expect(approvalDialog?.textContent).toContain('Registrando...');
    expect(approvalDialog?.querySelector('mat-icon.button-spinner')?.textContent?.trim()).toBe('progress_activity');
    expect(confirmButton.disabled).toBe(true);
    expect(nativeElement.querySelector<HTMLButtonElement>('[aria-label="Cerrar"]')?.disabled).toBe(true);
    expect(Array.from(nativeElement.querySelectorAll<HTMLButtonElement>('.approval-dialog footer button'))
      .find((button) => button.textContent?.includes('Cancelar'))?.disabled).toBe(true);

    resolveApproval(approvedRecord);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(nativeElement.querySelector('.approval-dialog')).toBeNull();
    expect(nativeElement.querySelector('.success-dialog')).not.toBeNull();
    expect(nativeElement.textContent).toContain('Iniciativa aprobada');
  });
});
