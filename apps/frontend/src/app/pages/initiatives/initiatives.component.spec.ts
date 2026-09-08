import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativesComponent } from './initiatives.component';

describe('InitiativesComponent pagination', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InitiativesComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('shows five initiatives and returns to the first page when filtering', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.initiatives()[0];
    repository.initiatives.set(Array.from({ length: 6 }, (_, index) => ({ ...template, code: `I-10${index}-2026`, name: `Iniciativa ${index + 1}` })));
    const fixture = TestBed.createComponent(InitiativesComponent);
    const component = fixture.componentInstance;

    expect(component.pagedInitiatives()).toHaveLength(5);
    component.pageIndex.set(1);
    expect(component.pagedInitiatives()).toHaveLength(1);

    component.filters.patchValue({ search: 'I-100-2026' });
    expect(component.currentPage()).toBe(0);
    expect(component.pagedInitiatives()).toHaveLength(1);
  });

  it('shows only initiative states in the status filter and keeps the list consultive', () => {
    const fixture = TestBed.createComponent(InitiativesComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const options = Array.from(nativeElement.querySelectorAll('select[formcontrolname="status"] option'))
      .map((option) => option.textContent?.trim());

    expect(options).toEqual(expect.arrayContaining(['Presentado', 'Iniciativa aprobada', 'Iniciativa archivada', 'No Admisible']));
    expect(options).not.toEqual(expect.arrayContaining(['Proyecto en ejecución', 'Producto aprobado', 'Suspendido', 'Finalizado', 'No Aplicable']));
    expect(nativeElement.textContent).not.toContain('Cambiar estado');
  });

  it('renders initiative states with the canonical icon, tone and accessible text', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.initiatives()[0];
    const expected = [
      ['Presentado', 'schedule', 'pending'],
      ['Iniciativa aprobada', 'check_circle', 'success'],
      ['Iniciativa archivada', 'archive', 'neutral'],
      ['No Admisible', 'cancel', 'danger'],
    ] as const;
    repository.initiatives.set(expected.map(([status], index) => ({ ...template, code: `I-STATUS-${index}-2026`, status })));

    const fixture = TestBed.createComponent(InitiativesComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const statusTags = Array.from(host.querySelectorAll<HTMLElement>('.status-tag'));

    expect(statusTags).toHaveLength(expected.length);
    expected.forEach(([status, icon, tone], index) => {
      const tag = statusTags[index]!;
      expect(tag.getAttribute('data-tone')).toBe(tone);
      expect(tag.textContent).toContain(status);
      expect(tag.querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
      expect(tag.querySelector('mat-icon')?.textContent?.trim()).toBe(icon);
    });
    expect(host.textContent).not.toContain('Proyecto en ejecución');
  });

  it('filtra por referencias resueltas de fuente y Unidad Orgánica conservando los filtros vigentes', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const target = repository.initiatives()[0];
    const fixture = TestBed.createComponent(InitiativesComponent);
    const component = fixture.componentInstance;
    component.filters.patchValue({ source: String(target.sourceReference?.id), unit: String(target.organizationalUnits?.[0]?.id) });

    expect(component.filteredInitiatives().map((item) => item.code)).toContain(target.code);
    expect(Object.keys(component.filters.getRawValue()).sort()).toEqual(['date', 'search', 'source', 'status', 'unit']);
  });

  it('muestra las siglas de todas las Unidades Orgánicas en orden y oculta el campo legado', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const first = { ...repository.organizationalUnits()[0], id: 101, name: 'Primera Unidad Orgánica', acronym: 'UO1' };
    const second = { ...first, id: 102, name: 'Segunda Unidad Orgánica', acronym: 'UO2' };
    repository.initiatives.set([{ ...repository.initiatives()[0], unit: 'Descripción legado', organizationalUnits: [first, second] }]);
    const fixture = TestBed.createComponent(InitiativesComponent);
    fixture.detectChanges();

    const cell = (fixture.nativeElement as HTMLElement).querySelector('tbody tr td:nth-child(5)') as HTMLElement;
    expect(cell.textContent).toContain('UO1');
    expect(cell.textContent).toContain('UO2');
    expect(cell.textContent).not.toContain('Primera Unidad Orgánica');
    expect(cell.textContent).not.toContain('Descripción legado');
    expect(Array.from(cell.querySelectorAll('.unit-chip')).map((chip) => chip.getAttribute('title'))).toEqual([first.name, second.name]);
    expect(Array.from(cell.querySelectorAll('.unit-chip')).map((chip) => chip.getAttribute('aria-label'))).toEqual([
      `Unidad Orgánica: ${first.name} (${first.acronym})`,
      `Unidad Orgánica: ${second.name} (${second.acronym})`,
    ]);
  });

  it('presenta carga, vacío y error de catálogos sin inventar opciones', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const fixture = TestBed.createComponent(InitiativesComponent);
    repository.catalogs.set({ phase: 'loading', value: repository.catalogs().value, error: null, requestId: 2 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Cargando opciones de filtro');

    repository.catalogs.set({ phase: 'ready', value: { ...repository.catalogs().value, sources: [] }, error: null, requestId: 3 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay opciones activas');

    repository.catalogs.set({ phase: 'error', value: repository.catalogs().value, error: 'Fuentes no disponibles', requestId: 4 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Fuentes no disponibles');
  });

  it('mantiene el listado consultivo: detalle y menú de acciones sin edición inline', () => {
    const fixture = TestBed.createComponent(InitiativesComponent);
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector('tbody tr') as HTMLElement;
    const detailLink = row.querySelector('a.secondary-button') as HTMLAnchorElement;

    expect(detailLink?.getAttribute('href')).toBe('/iniciativas/I-024-2026');
    expect(row.querySelector('button[aria-label="Acciones de I-024-2026"]')).not.toBeNull();
    expect(row.querySelector('a[href*="/editar"]')).toBeNull();
    expect(row.textContent).not.toContain('Editar');
    expect(row.textContent).toContain('Presentado');
  });
});
