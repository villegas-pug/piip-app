import { TestBed } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
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
      roleScopes: [{ role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 1 }],
      roles: ['ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1], institutionWide: false,
    });
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();

    const profile = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.profile-button')!;
    expect(profile.textContent).toContain('Cristopher Guevara Villegas');
    expect(profile.textContent).toContain('Administrador PIIP');
    expect(profile.getAttribute('aria-label')).toContain('Cristopher Guevara Villegas');
  });

  it('muestra el contador numérico y no marca avisos al activar la campana', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();

    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.notification-button')!;
    expect(button.textContent).toContain('3');
    await fixture.componentInstance.showNotifications();

    expect(navigateByUrl).toHaveBeenCalledWith('/inicio');
    expect(repository.notifications().filter((item) => !item.read)).toHaveLength(3);
  });

  it('lleva el foco a Mis notificaciones sin lectura automática', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const target = document.createElement('section');
    target.id = 'mis-notificaciones';
    target.tabIndex = -1;
    target.scrollIntoView = vi.fn();
    document.body.appendChild(target);
    try {
      const fixture = TestBed.createComponent(AppShellComponent);
      fixture.detectChanges();
      await fixture.componentInstance.showNotifications();
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(document.activeElement).toBe(target);
      expect(repository.notifications().filter((item) => !item.read)).toHaveLength(3);
    } finally {
      target.remove();
    }
  });

  it('recalculates the visible role from the active UE without combining grants', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'Unidad 1', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'Unidad 2', institutionId: 1 },
    ]);
    repository.currentUser.set({
      subject: 'mixed', fullName: 'Usuario mixto', email: 'mixed@example.pe',
      roleScopes: [
        { role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
      ], roles: ['CONSULTA_EXTERNA', 'ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1, 2], institutionWide: false,
    });
    const fixture = TestBed.createComponent(AppShellComponent);

    repository.selectedExecutingUnitId.set(1);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.profile-copy')?.textContent).toContain('Consulta externa');

    repository.selectedExecutingUnitId.set(2);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.profile-copy')?.textContent).toContain('Administrador PIIP');
  });

  it('keeps user administration visible but disabled and identifies its available UE', async () => {
    const repository = configureMixedScopes();
    repository.selectedExecutingUnitId.set(1);
    const overlay = TestBed.inject(OverlayContainer);
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.profile-button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const option = overlay.getContainerElement().querySelector<HTMLButtonElement>('[data-testid="user-administration-option"]');
    expect(option).not.toBeNull();
    expect(option?.disabled).toBe(true);
    expect(option?.textContent).toContain('Administrar usuarios');
    expect(option?.textContent).toContain('Disponible al seleccionar: UE-002');
  });

  it('hides user administration without any Administrator grant', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.toggleRole();
    const overlay = TestBed.inject(OverlayContainer);
    const fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.profile-button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(overlay.getContainerElement().querySelector('[data-testid="user-administration-option"]')).toBeNull();
  });

  it('leaves user administration and explains the active-scope change', async () => {
    const repository = configureMixedScopes();
    repository.selectedExecutingUnitId.set(2);
    const fixture = TestBed.createComponent(AppShellComponent);
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const open = vi.spyOn(MatSnackBar.prototype, 'open');
    const activity = TestBed.inject(PiipActivityService);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.currentUrl.set('/administracion/usuarios');

    expect(repository.canAdministerExecutingUnit(1)).toBe(false);
    expect(fixture.componentInstance.currentUrl()).toBe('/administracion/usuarios');
    expect(activity.isBlocking()).toBe(false);

    try {
      await fixture.componentInstance.selectExecutingUnit(1);

      expect(repository.canAdministerExecutingUnit(1)).toBe(false);
      expect(navigateByUrl).toHaveBeenCalledWith('/inicio');
      expect(open).toHaveBeenCalledWith(
        'Saliste de Administración de usuarios porque la UE activa no tiene rol Administrador PIIP.',
        'Cerrar',
        { duration: 5200 },
      );
      expect(repository.administrableScopes()).toEqual([]);
    } finally {
      open.mockRestore();
    }
  });

  function configureMixedScopes(): PiipMockRepository {
    const repository = TestBed.inject(PiipMockRepository);
    repository.executingUnits.set([
      { id: 1, code: 'UE-001', name: 'Unidad de consulta', institutionId: 1 },
      { id: 2, code: 'UE-002', name: 'Unidad administrable', institutionId: 1 },
    ]);
    repository.currentUser.set({
      subject: 'mixed', fullName: 'Usuario mixto', email: 'mixed@example.pe',
      roleScopes: [
        { role: 'CONSULTA_EXTERNA', institutionId: 1, executingUnitId: 1 },
        { role: 'ADMINISTRADOR_PIIP', institutionId: 1, executingUnitId: 2 },
      ], roles: ['CONSULTA_EXTERNA', 'ADMINISTRADOR_PIIP'], institutionIds: [1], executingUnitIds: [1, 2], institutionWide: false,
    });
    repository.administrableScopes.set([{
      institutionId: 1,
      institutionCode: 'MIDAGRI',
      institutionName: 'Ministerio de Desarrollo Agrario y Riego',
      institutionWideAllowed: true,
      executingUnits: [
        { id: 1, code: 'UE-001', name: 'Unidad de consulta' },
        { id: 2, code: 'UE-002', name: 'Unidad administrable' },
      ],
    }]);
    return repository;
  }
});
