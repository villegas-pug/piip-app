import { initiativeStatusVisual } from './initiative-status-visual';

describe('initiativeStatusVisual', () => {
  it('devuelve iconos y tonos semánticos para todos los estados de iniciativa', () => {
    expect(initiativeStatusVisual('PRESENTED')).toEqual({ icon: 'schedule', tone: 'pending' });
    expect(initiativeStatusVisual('INITIATIVE_APPROVED')).toEqual({ icon: 'check_circle', tone: 'success' });
    expect(initiativeStatusVisual('INITIATIVE_ARCHIVED')).toEqual({ icon: 'archive', tone: 'neutral' });
    expect(initiativeStatusVisual('NOT_ADMISSIBLE')).toEqual({ icon: 'cancel', tone: 'danger' });
  });

  it('usa una presentación neutral para un estado desconocido', () => {
    expect(initiativeStatusVisual('Estado desconocido')).toEqual({ icon: 'circle', tone: 'neutral' });
  });
});
