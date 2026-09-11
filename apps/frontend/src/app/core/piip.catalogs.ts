import { PiipStatus, PortfolioStatusOption, PortfolioStatusReference } from './piip.models';

export const PIIP_CATALOGS = {
  finalProductTypes: ['Prototipo de solución conceptualizada', 'Solución funcional', 'NA'],
  digitalComponents: ['Si', 'No'],
} as const;

/** Códigos de estado que pertenecen al contexto de una iniciativa. */
export type InitiativeStatus = Extract<PiipStatus, 'PRESENTED' | 'INITIATIVE_APPROVED' | 'INITIATIVE_ARCHIVED' | 'NOT_ADMISSIBLE'>;

/** Códigos de estado que pertenecen al contexto de un proyecto. */
export type ProjectStatus = Extract<PiipStatus, 'PROJECT_IN_PROGRESS' | 'PRODUCT_APPROVED' | 'PRODUCT_NOT_APPROVED' | 'SUSPENDED' | 'CANCELLED' | 'FINISHED'>;

/** Destinos admitidos por la matriz de iniciativa, agrupados por código de estado actual.
 *
 * La aprobación conserva su operación existente y no se duplica en este selector.
 */
export const INITIATIVE_STATUS_TRANSITIONS: Readonly<Partial<Record<InitiativeStatus, readonly InitiativeStatus[]>>> = {
  PRESENTED: ['INITIATIVE_APPROVED', 'NOT_ADMISSIBLE', 'INITIATIVE_ARCHIVED'],
  INITIATIVE_APPROVED: ['INITIATIVE_ARCHIVED'],
  INITIATIVE_ARCHIVED: [],
  NOT_ADMISSIBLE: [],
};

/** Destinos admitidos por la matriz de proyecto, agrupados por código de estado actual. */
export const PROJECT_STATUS_TRANSITIONS: Readonly<Partial<Record<ProjectStatus, readonly ProjectStatus[]>>> = {
  PROJECT_IN_PROGRESS: ['PRODUCT_APPROVED', 'PRODUCT_NOT_APPROVED', 'SUSPENDED', 'CANCELLED'],
  SUSPENDED: ['PROJECT_IN_PROGRESS', 'CANCELLED'],
  PRODUCT_NOT_APPROVED: ['PROJECT_IN_PROGRESS', 'CANCELLED'],
  PRODUCT_APPROVED: ['FINISHED'],
  CANCELLED: [],
  FINISHED: [],
};

/** Opciones activas del catálogo filtradas por aplicabilidad al tipo de registro. */
export function applicableStatusOptions(
  statuses: readonly PortfolioStatusOption[],
  applicability: 'INITIATIVE' | 'PROJECT',
): readonly PortfolioStatusOption[] {
  return statuses.filter((option) => option.active && option.applicability === applicability);
}

/** Denominación vigente de un estado a partir de su referencia (fallback al código). */
export function statusDisplayName(status: PortfolioStatusReference | undefined): string {
  return status?.name ?? status?.code ?? 'Sin información';
}

/** Resuelve la denominación de un código desde el catálogo; conserva el código como fallback neutral. */
export function resolveStatusName(statuses: readonly PortfolioStatusOption[], code: string | undefined): string {
  if (!code) return 'Sin información';
  return statuses.find((option) => option.code === code)?.name ?? code;
}
