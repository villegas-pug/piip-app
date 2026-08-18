import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('mantiene el resumen compacto en tres notificaciones y expande en línea', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const repository = TestBed.inject(PiipMockRepository);
    repository.loadHomePortfolio(component.query());
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.notification-row')).toHaveLength(3);
    (fixture.nativeElement.querySelector('.all-notifications') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.notification-row')).toHaveLength(4);
  });

  it('solo marca una notificación mediante la acción explícita de la fila', async () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    expect(repository.notifications().filter((item) => !item.read)).toHaveLength(3);
    await fixture.componentInstance.markAsRead(1);
    expect(repository.notifications().find((item) => item.id === 1)?.read).toBe(true);
    expect(repository.notifications().filter((item) => !item.read)).toHaveLength(2);
  });

  it('restablece el estado cuando el cambio de tipo lo vuelve inválido', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.componentInstance.changeStatus('Presentado');
    fixture.componentInstance.changeType('Proyecto');
    expect(fixture.componentInstance.query().status).toBe('Todos');
    expect(fixture.componentInstance.query().page).toBe(0);
  });

  it('muestra la UE activa, seis registros y conteos reconciliados', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.portfolioRecords.update((items) => [
      ...items,
      { ...items[0], code: 'I-099-2026', name: 'Registro adicional 1' },
      { ...items[1], code: 'I-098-2026', name: 'Registro adicional 2' },
    ]);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    repository.loadHomePortfolio(fixture.componentInstance.query());
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('UE-DEMO');
    expect(repository.homePortfolio().totalElements).toBe(6);
    expect(repository.homePortfolio().statusCounts.reduce((total, item) => total + item.count, 0)).toBe(6);
    expect(repository.homePortfolio().content).toHaveLength(5);
  });

  it('aplica filtros, reinicia la página, distingue sin resultados y espera 300 ms para buscar', () => {
    vi.useFakeTimers();
    try {
      const repository = TestBed.inject(PiipMockRepository);
      const fixture = TestBed.createComponent(DashboardComponent);
      fixture.detectChanges();
      const load = vi.spyOn(repository, 'loadHomePortfolio');
      fixture.componentInstance.query.update((value) => ({ ...value, page: 2, status: 'Presentado' }));
      fixture.componentInstance.onSearch('texto');
      expect(load).not.toHaveBeenCalled();
      vi.advanceTimersByTime(299);
      expect(load).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1);
      expect(load).toHaveBeenCalledOnce();
      expect(fixture.componentInstance.query().page).toBe(0);
      load.mockRestore();

      fixture.componentInstance.changeType('Proyecto');
      expect(fixture.componentInstance.query().status).toBe('Todos');
      fixture.componentInstance.changeStatus('Finalizado');
      expect(repository.homePortfolio().executingUnitTotalElements).toBeGreaterThan(0);
      expect(repository.homePortfolio().totalElements).toBe(0);
    } finally {
      vi.useRealTimers();
    }
  });
});
