import { initiativeStatusVisual } from './initiative-status-visual';

describe('initiativeStatusVisual', () => {
  it('devuelve iconos y tonos semánticos para todos los estados de iniciativa', () => {
    expect(initiativeStatusVisual('Presentado')).toEqual({ icon: 'schedule', tone: 'pending' });
    expect(initiativeStatusVisual('Iniciativa aprobada')).toEqual({ icon: 'check_circle', tone: 'success' });
    expect(initiativeStatusVisual('Iniciativa archivada')).toEqual({ icon: 'archive', tone: 'neutral' });
    expect(initiativeStatusVisual('No Admisible')).toEqual({ icon: 'cancel', tone: 'danger' });
  });

  it('usa una presentación neutral para un estado desconocido', () => {
    expect(initiativeStatusVisual('Estado desconocido')).toEqual({ icon: 'circle', tone: 'neutral' });
  });
});
