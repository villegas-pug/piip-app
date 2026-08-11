import { TestBed } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { provideRouter } from '@angular/router';
import { PiipActivityService } from '../core/piip-activity.service';
import { PiipMockRepository } from '../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../core/piip-repository.token';
import { AppShellComponent, isNavigationRouteActive } from './app-shell.component';

describe('PIIP shell navigation', () => {
  it('keeps Documentos active in the inbox and contextual dossier routes', () => {
    expect(isNavigationRouteActive('/documentos', '/documentos')).toBe(true);
    expect(isNavigationRouteActive('/documentos', '/iniciativas/I-024-2026/documentos')).toBe(true);
    expect(isNavigationRouteActive('/documentos', '/proyectos/P-005-2026/documentos?tab=gestion')).toBe(true);
  });

  it('does not activate Iniciativas or Proyectos inside a document dossier', () => {
    expect(isNavigationRouteActive('/iniciativas', '/iniciativas/I-024-2026/documentos')).toBe(false);
    expect(isNavigationRouteActive('/proyectos', '/proyectos/P-005-2026/documentos')).toBe(false);
  });
});

describe('AppShellComponent loading state', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('blocks duplicate Unidad Ejecutora changes until the refresh finishes', async () => {
    const repository = TestBed.inject(PIIP_REPOSITORY);
    const mockRepository = TestBed.inject(PiipMockRepository);
    const activity = TestBed.inject(PiipActivityService);
    mockRepository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'Unidad 1', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'Unidad 2', institutionId: 1 },
    ]);
    let releaseSelection!: () => void;
    const selection = new Promise<void>((resolve) => { releaseSelection = resolve; });
    const selectExecutingUnit = vi.spyOn(repository, 'selectExecutingUnit').mockReturnValue(selection);
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();

    const first = fixture.componentInstance.selectExecutingUnit(2);
    const duplicate = fixture.componentInstance.selectExecutingUnit(2);
    fixture.detectChanges();

    expect(selectExecutingUnit).toHaveBeenCalledTimes(1);
    expect(activity.isBlocking()).toBe(true);
    expect(activity.blockingMessage()).toContain('Unidad Ejecutora');
    expect((fixture.nativeElement.querySelector('.executing-unit-trigger') as HTMLButtonElement).disabled).toBe(true);

    releaseSelection();
    await Promise.all([first, duplicate]);
    expect(activity.isBlocking()).toBe(false);
  });

  it('renders the active scope and marks it in the authorized-units menu', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'Unidad de Innovación', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'Unidad de Desarrollo', institutionId: 1 },
    ]);
    const overlay = TestBed.inject(OverlayContainer);
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const trigger = nativeElement.querySelector<HTMLButtonElement>('.executing-unit-trigger')!;

    expect(trigger.textContent).toContain('UE-001');
    expect(trigger.textContent).toContain('Unidad de Innovación');
    expect(trigger.getAttribute('aria-label')).toContain('Unidad de Innovación');

    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const menuText = overlay.getContainerElement().textContent ?? '';
    expect(menuText).toContain('UE-001');
    expect(menuText).toContain('UE-002');
    expect(menuText).toContain('Unidad activa');
  });

  it('shows the authenticated user name above the effective role in the profile control', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.currentUser.set({
      subject: 'profile-subject', fullName: 'Cristopher Guevara Villegas', email: 'cristopher@example.pe',
      roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();

    const profile = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.profile-button')!;
    expect(profile.textContent).toContain('Cristopher Guevara Villegas');
    expect(profile.textContent).toContain('Administrador PIIP');
    expect(profile.getAttribute('aria-label')).toContain('Cristopher Guevara Villegas');
  });
});
