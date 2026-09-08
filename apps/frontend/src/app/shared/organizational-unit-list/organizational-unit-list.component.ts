import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, input, output, signal, viewChild, viewChildren } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { OrganizationalUnit, ResourcePhase } from '../../core/piip.models';

let nextComponentInstanceId = 0;

/** Valor de la lista emitido a los formularios consumidores. */
export interface OrganizationalUnitListValue {
  /** Unidades confirmadas, en orden de incorporación (posición de presentación 1..N). */
  readonly unitIds: readonly number[];
  /** Total de filas visibles, incluida una fila pendiente de selección. */
  readonly rowCount: number;
  /** Verdadero cuando existe una fila sin unidad elegida. */
  readonly hasPendingSelection: boolean;
}

/**
 * Lista dinámica y ordenada de "Unidades Orgánicas Involucradas" (feature 017, T007).
 * Filas Nro/Descripción/Abreviatura de solo lectura del maestro, incorporación al final
 * con control de selección, retiro salvo la última fila, renumeración automática 1..N,
 * opciones ya seleccionadas no reofrecidas, estados del catálogo con reintento,
 * errores por fila inyectados por el padre y operación completa por teclado.
 *
 * API pública (consumidores previstos: initiative-form, derived-project-form con
 * precarga editable, preexisting-project-form y portfolio-record-edit):
 *
 * Inputs:
 * - `catalog`: opciones seleccionables; el padre es dueño de la carga y del filtro
 *   funcional (unidades activas de la Unidad Ejecutora con sigla no vacía, FR-012).
 * - `catalogPhase`: fase de la carga del catálogo ('idle' | 'loading' | 'ready' | 'error').
 * - `catalogError`: mensaje mostrado cuando la fase es 'error'.
 * - `value`: valor de partida (unidades confirmadas con denominación del maestro);
 *   se aplica solo cuando su contenido cambia, así el padre puede precargar (proyecto
 *   derivado) o fijar el valor base (edición) sin reiniciar la edición del usuario.
 * - `label`: denominación visible del campo; por defecto "Unidades Orgánicas Involucradas".
 * - `rowErrors`: errores por fila inyectados por el padre (rechazos del backend que
 *   identifican fila), indexados por posición de fila (0-based).
 * - `disabled`: bloquea agregar, seleccionar y retirar (p. ej. durante el envío).
 *
 * Outputs:
 * - `listChange`: emite `{ unitIds, rowCount, hasPendingSelection }` tras cada cambio;
 *   el padre envía `responsibleUnits` como `unitIds.map(id => ({ organizationalUnitId: id }))`
 *   y bloquea la confirmación si `unitIds` está vacío o `hasPendingSelection` es true.
 * - `catalogRetry`: el usuario solicita recargar el catálogo; la carga es del padre.
 *
 * Restricciones de interacción: una sola fila pendiente de selección a la vez (el botón
 * "Agregar unidad" se deshabilita hasta elegirla); el retiro se bloquea cuando resta una
 * fila; el foco va al select nuevo al agregar y al botón "Agregar unidad" al confirmar
 * una selección o retirar una fila.
 */
@Component({
  selector: 'app-organizational-unit-list',
  imports: [MatIconModule],
  templateUrl: './organizational-unit-list.component.html',
  styleUrl: './organizational-unit-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationalUnitListComponent {
  // --- Inputs y outputs (API pública) ---
  readonly catalog = input.required<readonly OrganizationalUnit[]>();
  readonly catalogPhase = input<ResourcePhase>('idle');
  readonly catalogError = input<string | null>(null);
  readonly value = input<readonly OrganizationalUnit[]>([]);
  readonly label = input('Unidades Orgánicas Involucradas');
  readonly rowErrors = input<Readonly<Record<number, string>>>({});
  readonly disabled = input(false);

  readonly listChange = output<OrganizationalUnitListValue>();
  readonly catalogRetry = output<void>();

  // --- Estado interno ---
  /** Filas visibles: unidades confirmadas o una fila pendiente (`null`) al final. */
  readonly rows = signal<ReadonlyArray<OrganizationalUnit | null>>([null]);
  private lastSyncedValueIds: readonly number[] | null = null;
  /** Prefijo de IDs del DOM para mantener únicos los atributos ARIA entre instancias. */
  readonly instanceId = `ou-list-${nextComponentInstanceId++}`;

  private readonly pendingSelects = viewChildren<ElementRef<HTMLSelectElement>>('pendingSelect');
  private readonly addButton = viewChild.required<ElementRef<HTMLButtonElement>>('addButton');
  private readonly focusTarget = signal<'pending-select' | 'add-button' | null>(null);

  readonly confirmedUnits = computed(() => this.rows().flatMap((row) => (row ? [row] : [])));
  readonly hasPendingSelection = computed(() => this.rows().some((row) => row === null));
  /** Opciones del control de selección: catálogo sin las unidades ya confirmadas (FR-012). */
  readonly availableUnits = computed(() => {
    const selected = new Set(this.confirmedUnits().map((unit) => unit.id));
    return this.catalog().filter((unit) => !selected.has(unit.id));
  });
  readonly canRemoveRow = computed(() => this.rows().length > 1 && !this.disabled());
  readonly canAddRow = computed(() =>
    !this.disabled() && this.catalogPhase() === 'ready' && !this.hasPendingSelection() && this.availableUnits().length > 0);
  /** Causa del bloqueo del botón agregar; null cuando el estado del catálogo ya informa. */
  readonly addDisabledReason = computed<string | null>(() => {
    if (this.canAddRow()) return null;
    if (this.disabled() || this.catalogPhase() !== 'ready' || !this.catalog().length) return null;
    if (this.hasPendingSelection()) return 'Selecciona la unidad de la fila pendiente antes de agregar otra.';
    return 'No hay más Unidades Orgánicas disponibles para agregar.';
  });

  constructor() {
    // Sincroniza el valor de partida: aplica el input `value` solo cuando su contenido
    // cambia (evita reiniciar la edición del usuario por ecos del mismo valor) y no
    // emite durante la inicialización del componente.
    effect(() => {
      const incoming = this.value();
      const incomingIds = incoming.map((unit) => unit.id);
      const alreadySynced = this.lastSyncedValueIds !== null && idsEqual(incomingIds, this.lastSyncedValueIds);
      if (alreadySynced) return;
      const isFirstSync = this.lastSyncedValueIds === null;
      this.lastSyncedValueIds = incomingIds;
      this.rows.set(incoming.length ? [...incoming] : [null]);
      if (!isFirstSync) this.emitChange();
    });

    // Gestión de foco predecible: al agregar, foco en el select de la fila pendiente;
    // al confirmar la selección o retirar una fila, foco en el botón "Agregar unidad".
    effect(() => {
      const target = this.focusTarget();
      if (!target) return;
      if (target === 'pending-select') {
        const select = this.pendingSelects()[0]?.nativeElement;
        // Si la vista embebida de la fila aún no existe, el efecto reintenta al resolverse.
        if (!select) return;
        select.focus({ preventScroll: true });
      } else {
        // El botón puede haber perdido el foco al retirarse la fila activa; se difiere
        // hasta que Angular termine de reconciliar la vista de la tabla.
        queueMicrotask(() => this.addButton().nativeElement.focus({ preventScroll: true }));
      }
      this.focusTarget.set(null);
    });
  }

  addRow(): void {
    if (!this.canAddRow()) return;
    this.rows.update((rows) => [...rows, null]);
    this.focusTarget.set('pending-select');
    this.emitChange();
  }

  onSelectUnit(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const unit = this.catalog().find((option) => option.id === Number(select.value));
    if (!unit) return; // Opción placeholder: la fila permanece pendiente de selección.
    this.rows.update((rows) => rows.map((row) => (row === null ? unit : row)));
    this.focusTarget.set('add-button');
    this.emitChange();
  }

  removeRow(index: number): void {
    if (!this.canRemoveRow()) return;
    // La renumeración 1..N deriva de la posición: al compactar, los Nro se recalculan.
    this.rows.update((rows) => rows.filter((_, position) => position !== index));
    this.focusTarget.set('add-button');
    this.emitChange();
  }

  retryCatalog(): void {
    this.catalogRetry.emit();
  }

  rowErrorMessage(index: number): string | null {
    return this.rowErrors()[index] ?? null;
  }

  removeRowLabel(index: number): string {
    const row = this.rows()[index];
    return row ? `Retirar fila ${index + 1}: ${row.name}` : `Retirar fila ${index + 1}`;
  }

  pendingDescribedBy(index: number): string {
    const ids = [`${this.instanceId}-hint`];
    if (this.rowErrorMessage(index)) ids.push(`${this.instanceId}-row-${index}-error`);
    return ids.join(' ');
  }

  private emitChange(): void {
    this.listChange.emit({
      unitIds: this.confirmedUnits().map((unit) => unit.id),
      rowCount: this.rows().length,
      hasPendingSelection: this.hasPendingSelection(),
    });
  }
}

function idsEqual(first: readonly number[], second: readonly number[]): boolean {
  return first.length === second.length && first.every((id, index) => id === second[index]);
}
