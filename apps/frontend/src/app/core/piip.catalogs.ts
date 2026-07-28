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

export const RESPONSIBLE_UNITS = ['DGIA', 'DIPNA', 'DGA', 'DCLIMA', 'DGESEP', 'SENASA'] as const;
