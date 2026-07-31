import { TestBed } from '@angular/core/testing';
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
    const target = document.createElement('select');
    target.innerHTML = '<option value="2">Unidad 2</option>';
    target.value = '2';

    const first = fixture.componentInstance.selectExecutingUnit({ target } as unknown as Event);
    const duplicate = fixture.componentInstance.selectExecutingUnit({ target } as unknown as Event);
    fixture.detectChanges();

    expect(selectExecutingUnit).toHaveBeenCalledTimes(1);
    expect(activity.isBlocking()).toBe(true);
    expect(activity.blockingMessage()).toContain('Unidad Ejecutora');
    expect((fixture.nativeElement.querySelector('.executing-unit-selector select') as HTMLSelectElement).disabled).toBe(true);

    releaseSelection();
    await Promise.all([first, duplicate]);
    expect(activity.isBlocking()).toBe(false);
  });
});
