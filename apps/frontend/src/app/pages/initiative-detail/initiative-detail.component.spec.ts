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
});
