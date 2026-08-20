import type { PiipStatus } from '../../core/piip.models';

export type ProjectStatusTone = 'success' | 'progress' | 'neutral' | 'warning' | 'danger';

export interface ProjectStatusVisual {
  readonly icon: string;
  readonly tone: ProjectStatusTone;
}

const PROJECT_STATUS_VISUALS: Readonly<Partial<Record<PiipStatus, ProjectStatusVisual>>> = {
  'Producto aprobado': { icon: 'check_circle', tone: 'success' },
  Finalizado: { icon: 'check_circle', tone: 'success' },
  'Proyecto en ejecución': { icon: 'play_circle', tone: 'progress' },
  Suspendido: { icon: 'pause_circle', tone: 'warning' },
  'Producto no aprobado': { icon: 'cancel', tone: 'danger' },
  Cancelado: { icon: 'cancel', tone: 'danger' },
};

const FALLBACK_PROJECT_STATUS_VISUAL: ProjectStatusVisual = { icon: 'circle', tone: 'neutral' };

export function projectStatusVisual(status: PiipStatus | string): ProjectStatusVisual {
  return PROJECT_STATUS_VISUALS[status as PiipStatus] ?? FALLBACK_PROJECT_STATUS_VISUAL;
}
