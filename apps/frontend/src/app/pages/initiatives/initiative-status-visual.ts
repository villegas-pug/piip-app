import type { PiipStatus } from '../../core/piip.models';

export type InitiativeStatusTone = 'pending' | 'success' | 'neutral' | 'danger';

export interface InitiativeStatusVisual {
  readonly icon: string;
  readonly tone: InitiativeStatusTone;
}

const INITIATIVE_STATUS_VISUALS: Readonly<Partial<Record<PiipStatus, InitiativeStatusVisual>>> = {
  Presentado: { icon: 'schedule', tone: 'pending' },
  'Iniciativa aprobada': { icon: 'check_circle', tone: 'success' },
  'Iniciativa archivada': { icon: 'archive', tone: 'neutral' },
  'No Admisible': { icon: 'cancel', tone: 'danger' },
};

const FALLBACK_INITIATIVE_STATUS_VISUAL: InitiativeStatusVisual = { icon: 'circle', tone: 'neutral' };

export function initiativeStatusVisual(status: PiipStatus | string): InitiativeStatusVisual {
  return INITIATIVE_STATUS_VISUALS[status as PiipStatus] ?? FALLBACK_INITIATIVE_STATUS_VISUAL;
}
