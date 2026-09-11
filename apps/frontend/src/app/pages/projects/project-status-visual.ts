import type { PiipStatus } from '../../core/piip.models';

export type ProjectStatusTone = 'success' | 'progress' | 'neutral' | 'warning' | 'danger';

export interface ProjectStatusVisual {
  readonly icon: string;
  readonly tone: ProjectStatusTone;
}

const PROJECT_STATUS_VISUALS: Readonly<Partial<Record<PiipStatus, ProjectStatusVisual>>> = {
  PRODUCT_APPROVED: { icon: 'check_circle', tone: 'success' },
  FINISHED: { icon: 'check_circle', tone: 'success' },
  PROJECT_IN_PROGRESS: { icon: 'play_circle', tone: 'progress' },
  SUSPENDED: { icon: 'pause_circle', tone: 'warning' },
  PRODUCT_NOT_APPROVED: { icon: 'cancel', tone: 'danger' },
  CANCELLED: { icon: 'cancel', tone: 'danger' },
};

const FALLBACK_PROJECT_STATUS_VISUAL: ProjectStatusVisual = { icon: 'circle', tone: 'neutral' };

export function projectStatusVisual(status: PiipStatus | string): ProjectStatusVisual {
  return PROJECT_STATUS_VISUALS[status as PiipStatus] ?? FALLBACK_PROJECT_STATUS_VISUAL;
}
