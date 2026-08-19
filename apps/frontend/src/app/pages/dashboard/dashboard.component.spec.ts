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

  it('representa cada estado una sola vez en un gráfico con alternativa textual', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    repository.loadHomePortfolio(fixture.componentInstance.query());
    fixture.detectChanges();

    const statusCounts = repository.homePortfolio().statusCounts;
    const chart = fixture.nativeElement.querySelector('.status-chart') as HTMLElement;
    const chartRows = Array.from(chart.querySelectorAll<HTMLElement>('.chart-row'));

    expect(fixture.nativeElement.querySelector('.status-cards')).toBeNull();
    expect(chart.getAttribute('role')).toBe('list');
    expect(chartRows).toHaveLength(statusCounts.length);
    statusCounts.forEach((item, index) => {
      const chartRow = chartRows[index]!;
      expect(chartRow.getAttribute('role')).toBe('listitem');
      expect(chartRow.getAttribute('aria-label')).toBe(`${item.status}: ${item.count} registros`);
    });
  });

  it('asigna el icono y tono canónico a cada estado y usa fallback neutral', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    const expected = [
      ['Presentado', 'schedule', 'pending'],
      ['Iniciativa aprobada', 'check_circle', 'success'],
      ['Producto aprobado', 'check_circle', 'success'],
      ['Finalizado', 'check_circle', 'success'],
      ['Proyecto en ejecución', 'play_circle', 'progress'],
      ['Iniciativa archivada', 'archive', 'neutral'],
      ['No Aplicable', 'remove_circle_outline', 'neutral'],
      ['Suspendido', 'pause_circle', 'warning'],
      ['Producto no aprobado', 'cancel', 'danger'],
      ['No Admisible', 'cancel', 'danger'],
      ['Cancelado', 'cancel', 'danger'],
    ] as const;

    expected.forEach(([status, icon, tone]) => {
      expect(component.statusVisual(status)).toEqual({ icon, tone });
    });
    expect(component.statusVisual('Estado desconocido')).toEqual({ icon: 'circle', tone: 'neutral' });
  });

  it('renderiza iconos decorativos y tones únicamente en etiquetas de estado', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    repository.loadHomePortfolio(fixture.componentInstance.query());
    fixture.detectChanges();

    const records = repository.homePortfolio().content;
    const statusTags = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.status-tag'));
    const chartStatuses = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.chart-status'));

    expect(statusTags).toHaveLength(records.length);
    records.forEach((record, index) => {
      const visual = fixture.componentInstance.statusVisual(record.status);
      const tag = statusTags[index]!;
      expect(tag.getAttribute('data-tone')).toBe(visual.tone);
      expect(tag.querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
      expect(tag.querySelector('mat-icon')?.textContent?.trim()).toBe(visual.icon);
    });
    chartStatuses.forEach((tag) => {
      expect(tag.getAttribute('data-tone')).not.toBeNull();
      expect(tag.querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
    });
    expect(fixture.nativeElement.querySelector('.chart-fill[data-tone]')).toBeNull();
  });

  it('permite contraer la distribución conservando título y total', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    repository.loadHomePortfolio(fixture.componentInstance.query());
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector('.distribution-toggle') as HTMLButtonElement;
    const total = fixture.nativeElement.querySelector('.distribution-total') as HTMLElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('.status-chart')).not.toBeNull();
    expect(total.getAttribute('aria-label')).toBe(`Total filtrado: ${repository.homePortfolio().totalElements} registros`);
    expect(total.querySelector('.distribution-total-icon mat-icon')?.textContent?.trim()).toBe('analytics');
    expect(total.querySelector('.distribution-total-label')?.textContent).toContain('Total filtrado');
    expect(total.querySelector('.distribution-total-number')?.textContent).toContain(`${repository.homePortfolio().totalElements}`);
    expect(total.querySelector('.distribution-total-unit')?.textContent).toContain('registros');

    toggle.click();
    fixture.detectChanges();

    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect((fixture.nativeElement.querySelector('.status-chart') as HTMLElement).hidden).toBe(true);
    expect(fixture.nativeElement.querySelector('.distribution-title').textContent).toContain('Distribución por estado');
    expect(fixture.nativeElement.querySelector('.distribution-total')).toBe(total);
  });

  it('expone controles y registros con semántica accesible', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    repository.loadHomePortfolio(fixture.componentInstance.query());
    fixture.detectChanges();

    const notificationFilters = fixture.nativeElement.querySelector('.notification-tabs') as HTMLElement;
    const notificationButtons = notificationFilters.querySelectorAll<HTMLButtonElement>('button');
    const table = fixture.nativeElement.querySelector('.portfolio-table') as HTMLElement;
    const firstDetailLink = fixture.nativeElement.querySelector('.cell-action a') as HTMLAnchorElement;
    const firstRecord = repository.homePortfolio().content[0]!;

    expect(notificationFilters.getAttribute('role')).toBe('group');
    expect(notificationButtons.item(0).getAttribute('aria-pressed')).toBe('true');
    expect(notificationButtons.item(1).getAttribute('aria-pressed')).toBe('false');
    expect(fixture.nativeElement.querySelector('.unread-count').getAttribute('aria-live')).toBe('polite');
    expect(table.querySelectorAll('[role="columnheader"]')).toHaveLength(6);
    expect(table.querySelectorAll('[role="cell"]')).toHaveLength(repository.homePortfolio().content.length * 6);
    expect(firstDetailLink.getAttribute('aria-label')).toContain(firstRecord.code);
  });

  it('muestra el contexto local solo cuando existe una única Unidad Ejecutora', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('.single-unit-chip') as HTMLElement;
    expect(chip.textContent).toContain('UE-DEMO');
    expect(chip.getAttribute('aria-label')).toContain('Unidad Ejecutora activa');

    repository.executingUnits.update((units) => [...units, { id: 2, code: 'UE-002', name: 'Unidad adicional', institutionId: 1 }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.single-unit-chip')).toBeNull();
  });

  it('humaniza el tipo técnico y presenta la fecha en español de Perú', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const createdAt = '2026-08-18T21:30:00Z';
    repository.notifications.update((items) => [{ ...items[0]!, type: 'TAREA_CREADA', createdAt }]);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const firstNotification = fixture.nativeElement.querySelector('.notification-copy') as HTMLElement;
    const expectedDate = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(createdAt));
    expect(firstNotification.querySelector('strong')?.textContent).toContain('Tarea creada');
    expect(firstNotification.querySelector('small')?.textContent).toContain(expectedDate);
    expect(repository.notifications()[0]!.type).toBe('TAREA_CREADA');
  });

  it('limpia únicamente la búsqueda y conserva el debounce de 300 ms', () => {
    vi.useFakeTimers();
    try {
      const repository = TestBed.inject(PiipMockRepository);
      const fixture = TestBed.createComponent(DashboardComponent);
      fixture.detectChanges();
      const load = vi.spyOn(repository, 'loadHomePortfolio');
      fixture.componentInstance.query.set({ executingUnitId: 1, q: 'I-024', type: 'Iniciativa', status: 'Presentado', page: 2, size: 5 });
      fixture.detectChanges();

      const search = fixture.nativeElement.querySelector('.search-field') as HTMLElement;
      const clear = search.querySelector('.search-clear') as HTMLButtonElement;
      const filterLabels = Array.from(fixture.nativeElement.querySelectorAll<HTMLElement>('.filter-label')).map((label) => label.textContent?.trim());
      expect(filterLabels).toEqual(['Buscar por código o nombre', 'Tipo', 'Estado']);
      expect(search.textContent).toContain('Buscar por código o nombre');
      expect(search.querySelector('input')?.getAttribute('placeholder')).toBe('Código o nombre');
      expect(clear.getAttribute('aria-label')).toBe('Limpiar búsqueda');

      clear.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.query()).toMatchObject({ q: '', type: 'Iniciativa', status: 'Presentado', page: 0 });
      expect(load).not.toHaveBeenCalled();
      vi.advanceTimersByTime(299);
      expect(load).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1);
      expect(load).toHaveBeenCalledOnce();
      expect(fixture.nativeElement.querySelector('.search-clear')).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it('presenta Tipo y Estado con el mismo control base y permite limpiar sus valores', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    fixture.componentInstance.query.update((current) => ({ ...current, type: 'Iniciativa', status: 'Presentado', page: 2 }));
    fixture.detectChanges();

    const filterFields = fixture.nativeElement.querySelectorAll('.filter-field');
    const filterControls = fixture.nativeElement.querySelectorAll('.filter-control');
    const clearFilters = fixture.nativeElement.querySelector('.clear-button') as HTMLButtonElement;
    expect(filterFields).toHaveLength(3);
    expect(filterControls).toHaveLength(3);

    clearFilters.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.query()).toMatchObject({ type: 'Todos', status: 'Todos', page: 0 });
    expect(fixture.nativeElement.querySelector('.clear-button')).toBeNull();
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
