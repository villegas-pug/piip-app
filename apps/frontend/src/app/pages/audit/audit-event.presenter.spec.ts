import { AuditEvent } from '../../core/piip.models';
import { presentAuditEvent } from './audit-event.presenter';

describe('presentAuditEvent', () => {
  const baseEvent: AuditEvent = {
    recordCode: 'I-001-2026', timestamp: '31/07/2026\n12:44', event: 'DOCUMENTO_CARGADO', user: 'Ana Analista',
    email: 'ana@midagri.gob.pe', observation: '{"tipo":"INITIATIVE_TECHNICAL_OPINION","version":1}', actorSubject: 'subject-ana',
    rawDetail: '{"tipo":"INITIATIVE_TECHNICAL_OPINION","version":1}', icon: 'history',
  };

  it('creates a functional summary while preserving the original technical JSON', () => {
    const event = presentAuditEvent(baseEvent);

    expect(event.eventLabel).toBe('Documento cargado');
    expect(event.observation).toBe('Se cargó Informe de opinión técnica de evaluación de iniciativa, versión 1.');
    expect(event.detailFields).toEqual(expect.arrayContaining([{ label: 'Tipo documental', value: 'Informe de opinión técnica de evaluación de iniciativa' }]));
    expect(event.technicalDetail).toContain('"INITIATIVE_TECHNICAL_OPINION"');
  });

  it('presents initiative registration with its functional label and initial status', () => {
    const event = presentAuditEvent({
      ...baseEvent,
      event: 'INICIATIVA_REGISTRADA',
      observation: '{"estado":"Presentado"}',
      rawDetail: '{"estado":"Presentado"}',
    });

    expect(event.eventLabel).toBe('Iniciativa registrada');
    expect(event.observation).toBe('Estado inicial: Presentado.');
  });

  it('uses a safe fallback for unknown events and invalid technical details', () => {
    const event = presentAuditEvent({ ...baseEvent, event: 'EVENTO_DESCONOCIDO', observation: '{no es json}', rawDetail: '{no es json}' });

    expect(event.eventLabel).toBe('Evento Desconocido');
    expect(event.observation).toBe('Evento registrado.');
    expect(event.technicalDetail).toBe('{no es json}');
    expect(event.detailFields).toEqual([]);
  });

  it('presents portfolio status transitions with both states and the observation', () => {
    const event = presentAuditEvent({
      ...baseEvent,
      event: 'ESTADO_PROYECTO_CAMBIADO',
      observation: '{"estadoAnterior":"Proyecto en ejecución","estadoNuevo":"Finalizado","observacion":"Cierre validado"}',
      rawDetail: '{"estadoAnterior":"Proyecto en ejecución","estadoNuevo":"Finalizado","observacion":"Cierre validado"}',
    });

    expect(event.eventLabel).toBe('Estado de proyecto cambiado');
    expect(event.observation).toBe('El proyecto cambió de Proyecto en ejecución a Finalizado. Observación: Cierre validado');
    expect(event.detailFields).toEqual(expect.arrayContaining([
      { label: 'Estado anterior', value: 'Proyecto en ejecución' },
      { label: 'Estado nuevo', value: 'Finalizado' },
    ]));
  });
});
