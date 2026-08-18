import { PiipStatus } from './piip.models';

export const PIIP_CATALOGS = {
  recordTypes: ['Iniciativa', 'Proyecto'],
  solutionTypes: ['Solución potencial o adaptable', 'Solución por definir', 'No aplica'],
  sources: [
    'Ficha de iniciativa de innovación pública',
    'Concurso interno',
    'Innovación abierta',
    'Propuesta de jefatura o directivos',
    'Otros',
    'Convocatoria',
  ],
  statuses: [
    'Presentado',
    'Iniciativa aprobada',
    'Iniciativa archivada',
    'Proyecto en ejecución',
    'Producto aprobado',
    'Producto no aprobado',
    'Suspendido',
    'Cancelado',
    'Finalizado',
    'No Aplicable',
    'No Admisible',
  ] as PiipStatus[],
  finalProductTypes: ['Prototipo de solución conceptualizada', 'Solución funcional', 'NA'],
  digitalComponents: ['Si', 'No'],
} as const;

/** Estados que pertenecen al contexto de una iniciativa.
 *
 * El catálogo global se conserva para visualización y compatibilidad; estas
 * listas son las únicas que deben alimentar filtros y acciones contextuales.
 */
export const INITIATIVE_STATUSES = [
  'Presentado',
  'Iniciativa aprobada',
  'Iniciativa archivada',
  'No Admisible',
] as const satisfies readonly PiipStatus[];

/** Estados que pertenecen al contexto de un proyecto. */
export const PROJECT_STATUSES = [
  'Proyecto en ejecución',
  'Producto aprobado',
  'Producto no aprobado',
  'Suspendido',
  'Cancelado',
  'Finalizado',
] as const satisfies readonly PiipStatus[];

export type InitiativeStatus = typeof INITIATIVE_STATUSES[number];
export type ProjectStatus = typeof PROJECT_STATUSES[number];

/** Destinos admitidos por la matriz de iniciativa, agrupados por estado actual. */
export const INITIATIVE_STATUS_TRANSITIONS: Readonly<Record<InitiativeStatus, readonly InitiativeStatus[]>> = {
  // La aprobación conserva su operación existente y no se duplica en este selector.
  Presentado: ['Iniciativa archivada', 'No Admisible'],
  'Iniciativa aprobada': ['Iniciativa archivada'],
  'Iniciativa archivada': [],
  'No Admisible': [],
};

/** Destinos admitidos por la matriz de proyecto, agrupados por estado actual. */
export const PROJECT_STATUS_TRANSITIONS: Readonly<Record<ProjectStatus, readonly ProjectStatus[]>> = {
  'Proyecto en ejecución': ['Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado'],
  Suspendido: ['Proyecto en ejecución', 'Cancelado'],
  'Producto no aprobado': ['Proyecto en ejecución', 'Cancelado'],
  'Producto aprobado': ['Finalizado'],
  Cancelado: [],
  Finalizado: [],
};

export const RESPONSIBLE_UNITS = ['DGIA', 'DIPNA', 'DGA', 'DCLIMA', 'DGESEP', 'SENASA'] as const;
