import { Component, signal, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrganizationalUnit, ResourcePhase } from '../../core/piip.models';
import { OrganizationalUnitListComponent, OrganizationalUnitListValue } from './organizational-unit-list.component';

/** Host de prueba: ejerce el binding real de inputs/outputs del componente de lista. */
@Component({
  imports: [OrganizationalUnitListComponent],
  template: `<app-organizational-unit-list
    [catalog]="catalog()"
    [catalogPhase]="phase()"
    [catalogError]="catalogError()"
    [value]="value()"
    [label]="label()"
    [rowErrors]="rowErrors()"
    [disabled]="disabled()"
    (listChange)="onListChange($event)"
    (catalogRetry)="retries = retries + 1" />`,
})
class HostComponent {
  readonly catalog = signal<OrganizationalUnit[]>([]);
  readonly phase = signal<ResourcePhase>('ready');
  readonly catalogError = signal<string | null>(null);
  readonly value = signal<readonly OrganizationalUnit[]>([]);
  readonly label = signal('Unidades Orgánicas Involucradas');
  readonly rowErrors = signal<Readonly<Record<number, string>>>({});
  readonly disabled = signal(false);
  readonly list = viewChild.required(OrganizationalUnitListComponent);
  readonly listChanges: OrganizationalUnitListValue[] = [];
  retries = 0;

  onListChange(value: OrganizationalUnitListValue): void {
    this.listChanges.push(value);
  }
}

describe('OrganizationalUnitListComponent', () => {
  const UNITS = [unit(101), unit(202), unit(303)];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
  });

  it('muestra una lista ordenada con denominación, posición y abreviatura visibles', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0], UNITS[1]]);
    fixture.detectChanges();

    expect(element.querySelector('.ou-caption')?.textContent?.trim()).toBe('Unidades Orgánicas Involucradas');
    expect(element.querySelector('ol.ou-rows')).toBeTruthy();
    expect(element.querySelector('table')).toBeNull();
    expect(element.querySelectorAll('.ou-row')).toHaveLength(2);
    expect(Array.from(element.querySelectorAll('.ou-acronym-value')).map((cell) => cell.textContent?.trim())).toEqual(['U101', 'U202']);

    const removeLabels = Array.from(element.querySelectorAll('button.ou-remove')).map((button) => button.getAttribute('aria-label'));
    expect(removeLabels).toEqual(['Retirar fila 1: Unidad 101', 'Retirar fila 2: Unidad 202']);

    host.label.set('Otra denominación de edición');
    fixture.detectChanges();
    expect(element.querySelector('.ou-caption')?.textContent?.trim()).toBe('Otra denominación de edición');
  });

  it('renumera automáticamente la secuencia 1..N al retirar una fila intermedia', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0], UNITS[1], UNITS[2]]);
    fixture.detectChanges();

    element.querySelectorAll<HTMLButtonElement>('button.ou-remove')[1].click();
    fixture.detectChanges();

    expect(visibleNumbers(element)).toEqual(['1', '2']);
    expect(host.listChanges[host.listChanges.length - 1]).toEqual({ unitIds: [101, 303], rowCount: 2, hasPendingSelection: false });
  });

  it('bloquea el retiro cuando solo resta una fila', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();

    expect(element.querySelector<HTMLButtonElement>('button.ou-remove')?.disabled).toBe(true);

    const emissionsBefore = host.listChanges.length;
    host.list().removeRow(0); // Guarda interna: el retiro de la última fila no está disponible.
    fixture.detectChanges();

    expect(element.querySelectorAll('.ou-row')).toHaveLength(1);
    expect(host.listChanges.length).toBe(emissionsBefore);
  });

  it('agrega una fila pendiente al final sin reofrecer unidades ya seleccionadas', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('button.ou-add')?.click();
    fixture.detectChanges();

    const select = element.querySelector<HTMLSelectElement>('select.ou-select');
    expect(select).toBeTruthy();
    expect(select?.getAttribute('aria-label')).toBe('Unidad Orgánica de la fila 2');
    expect(select?.getAttribute('aria-required')).toBe('true');
    expect(select?.getAttribute('aria-describedby')).toContain('-hint');
    // La unidad 101 ya está seleccionada en la fila 1: no se reofrece (FR-012).
    const optionValues = Array.from(select?.options ?? []).map((option) => option.value);
    expect(optionValues).toEqual(['', '202', '303']);
    expect(host.listChanges[host.listChanges.length - 1]).toEqual({ unitIds: [101], rowCount: 2, hasPendingSelection: true });
  });

  it('solo permite una fila pendiente de selección a la vez', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button.ou-add')?.click();
    fixture.detectChanges();

    expect(element.querySelector<HTMLButtonElement>('button.ou-add')?.disabled).toBe(true);
    expect(element.textContent).toContain('Selecciona la unidad de la fila pendiente antes de agregar otra.');

    host.list().addRow(); // Guarda interna: no abre una segunda fila pendiente.
    fixture.detectChanges();
    expect(host.list().rows()).toHaveLength(2);
  });

  it('confirma la selección de la fila pendiente y conserva el orden de incorporación', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button.ou-add')?.click();
    fixture.detectChanges();

    const select = element.querySelector<HTMLSelectElement>('select.ou-select')!;
    select.value = '202';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(element.querySelector('select.ou-select')).toBeNull();
    expect(visibleNumbers(element)).toEqual(['1', '2']);
    expect(unitNames(element)).toEqual(['Unidad 101', 'Unidad 202']);
    expect(host.listChanges[host.listChanges.length - 1]).toEqual({ unitIds: [101, 202], rowCount: 2, hasPendingSelection: false });
  });

  it('informa cargando, vacío y error del catálogo con reintento', () => {
    const { host, fixture, element } = setup();
    host.phase.set('loading');
    host.value.set([UNITS[0]]);
    fixture.detectChanges();
    expect(element.textContent).toContain('Cargando Unidades Orgánicas...');
    expect(element.querySelector<HTMLButtonElement>('button.ou-add')?.disabled).toBe(true);

    host.phase.set('ready');
    host.catalog.set([]);
    fixture.detectChanges();
    expect(element.textContent).toContain('No hay Unidades Orgánicas disponibles para la Unidad Ejecutora.');
    expect(element.querySelector<HTMLButtonElement>('button.ou-add')?.disabled).toBe(true);

    host.phase.set('error');
    host.catalogError.set('Catálogo de Unidades Orgánicas no disponible');
    fixture.detectChanges();
    expect(element.querySelector('.ou-state')?.getAttribute('role')).toBe('alert');
    expect(element.textContent).toContain('Catálogo de Unidades Orgánicas no disponible');
    element.querySelector<HTMLButtonElement>('button.ou-retry')?.click();
    expect(host.retries).toBe(1);
  });

  it('muestra los errores por fila inyectados sin perder las selecciones válidas', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0], UNITS[1]]);
    host.rowErrors.set({ 1: 'La unidad ya no está disponible para nuevas asociaciones.' });
    fixture.detectChanges();

    const error = element.querySelector('small.ou-row-error');
    expect(error?.getAttribute('role')).toBe('alert');
    expect(error?.getAttribute('id')).toContain('-row-1-error');
    expect(error?.textContent).toContain('Fila 2: La unidad ya no está disponible para nuevas asociaciones.');
    expect(unitNames(element)).toEqual(['Unidad 101', 'Unidad 202']);
  });

  it('gestiona el foco al agregar, confirmar y retirar filas', async () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('button.ou-add')?.click();
    fixture.detectChanges();
    const select = element.querySelector<HTMLSelectElement>('select.ou-select')!;
    await vi.waitFor(() => { expect(document.activeElement).toBe(select); });

    select.value = '202';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const addButton = element.querySelector<HTMLButtonElement>('button.ou-add')!;
    await vi.waitFor(() => { expect(document.activeElement).toBe(addButton); });

    element.querySelectorAll<HTMLButtonElement>('button.ou-remove')[0].click();
    fixture.detectChanges();
    await vi.waitFor(() => { expect(document.activeElement).toBe(addButton); });
    expect(visibleNumbers(element)).toEqual(['1']);
  });

  it('aplica el valor de partida y solo lo reemplaza cuando su contenido cambia', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    fixture.detectChanges();
    // Sin valor de partida: la lista inicia con una fila pendiente de selección.
    expect(element.querySelector('select.ou-select')).toBeTruthy();

    // Precarga (proyecto derivado / valor base de edición).
    host.value.set([UNITS[0], UNITS[1]]);
    fixture.detectChanges();
    expect(visibleNumbers(element)).toEqual(['1', '2']);
    expect(unitNames(element)).toEqual(['Unidad 101', 'Unidad 202']);

    // El usuario retira la fila 2.
    element.querySelectorAll<HTMLButtonElement>('button.ou-remove')[1].click();
    fixture.detectChanges();
    expect(host.listChanges[host.listChanges.length - 1]).toEqual({ unitIds: [101], rowCount: 1, hasPendingSelection: false });

    // Un eco del mismo valor (nueva referencia, mismos ids) no reinicia la edición.
    host.value.set([unit(101), unit(202)]);
    fixture.detectChanges();
    expect(visibleNumbers(element)).toEqual(['1']);
    expect(host.list().rows()).toHaveLength(1);

    // Un contenido distinto sí reemplaza el valor.
    host.value.set([UNITS[2]]);
    fixture.detectChanges();
    expect(host.listChanges[host.listChanges.length - 1]).toEqual({ unitIds: [303], rowCount: 1, hasPendingSelection: false });
    expect(unitNames(element)).toEqual(['Unidad 303']);
  });

  it('bloquea toda la interacción cuando el formulario está deshabilitado', () => {
    const { host, fixture, element } = setup();
    host.catalog.set(UNITS);
    host.value.set([UNITS[0]]);
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button.ou-add')?.click();
    fixture.detectChanges();

    host.disabled.set(true);
    fixture.detectChanges();

    expect(element.querySelector<HTMLButtonElement>('button.ou-add')?.disabled).toBe(true);
    expect(element.querySelector<HTMLButtonElement>('button.ou-remove')?.disabled).toBe(true);
    expect(element.querySelector<HTMLSelectElement>('select.ou-select')?.disabled).toBe(true);
  });
});

function setup(): { host: HostComponent; fixture: ComponentFixture<HostComponent>; element: HTMLElement } {
  const fixture = TestBed.createComponent(HostComponent);
  return { host: fixture.componentInstance, fixture, element: fixture.nativeElement as HTMLElement };
}

function unit(id: number, overrides: Partial<OrganizationalUnit> = {}): OrganizationalUnit {
  return { id, code: `UO-${id}`, name: `Unidad ${id}`, acronym: `U${id}`, parentId: null, executingUnitId: 1, active: true, ...overrides };
}

function visibleNumbers(element: HTMLElement): string[] {
  return Array.from(element.querySelectorAll('.ou-number .ou-number-value')).map((cell) => cell.textContent?.trim() ?? '');
}

function unitNames(element: HTMLElement): string[] {
  return Array.from(element.querySelectorAll('.ou-unit-name')).map((cell) => cell.textContent?.trim() ?? '');
}
