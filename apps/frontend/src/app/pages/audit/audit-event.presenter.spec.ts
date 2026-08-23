import { AuditEvent } from '../../core/piip.models';
import { presentAuditEvent } from './audit-event.presenter';

describe('presentAuditEvent', () => {
  const baseEvent: AuditEvent = {
    recordCode: 'I-001-2026', timestamp: '31/07/2026\n12:44', event: 'DOCUMENTO_CARGADO', user: 'Ana Analista',
    email: 'ana@midagri.gob.pe', observation: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Informe de opinión técnica de evaluación de iniciativa","version":1}', actorSubject: 'subject-ana',
    rawDetail: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Informe de opinión técnica de evaluación de iniciativa","version":1}', icon: 'history',
  };

  it('creates a functional summary while preserving the original technical JSON', () => {
    const event = presentAuditEvent(baseEvent);

    expect(event.eventLabel).toBe('Documento cargado');
    expect(event.observation).toBe('Se cargó Informe de opinión técnica de evaluación de iniciativa, versión 1.');
    expect(event.detailFields).toEqual(expect.arrayContaining([
      { label: 'Código de tipo documental', value: 'INITIATIVE_TECHNICAL_OPINION' },
      { label: 'Tipo documental', value: 'Informe de opinión técnica de evaluación de iniciativa' },
    ]));
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

  it('presenta el snapshot documental recibido sin sustituirlo por un mapa local', () => {
    const event = presentAuditEvent({
      ...baseEvent,
      observation: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Nombre histórico recibido del backend","version":4}',
      rawDetail: '{"tipoCodigo":"INITIATIVE_TECHNICAL_OPINION","tipoNombre":"Nombre histórico recibido del backend","version":4}',
    });

    expect(event.observation).toBe('Se cargó Nombre histórico recibido del backend, versión 4.');
    expect(event.detailFields).toContainEqual({ label: 'Tipo documental', value: 'Nombre histórico recibido del backend' });
    expect(event.technicalDetail).toContain('Nombre histórico recibido del backend');
  });

  it('presenta actualizaciones de proyecto con etiquetas funcionales y diff legible', () => {
    const event = presentAuditEvent({
      ...baseEvent,
      recordCode: 'P-004-2026',
      event: 'PROYECTO_ACTUALIZADO',
      observation: JSON.stringify({
        tipoRegistro: 'Proyecto', unidadEjecutoraId: 1, unidadEjecutora: 'UE Demo', versionAnterior: 4, versionNueva: 5,
        cambios: {
          name: { anterior: 'Proyecto anterior', nuevo: 'Proyecto actualizado' },
          responsibleUnits: { anterior: [{ id: 10, code: 'UO-10' }], nuevo: [{ id: 11, code: 'UO-11' }] },
        }, resultado: 'EXITOSO',
      }),
      rawDetail: JSON.stringify({
        tipoRegistro: 'Proyecto', unidadEjecutoraId: 1, unidadEjecutora: 'UE Demo', versionAnterior: 4, versionNueva: 5,
        cambios: { name: { anterior: 'Proyecto anterior', nuevo: 'Proyecto actualizado' }, responsibleUnits: { anterior: [{ id: 10 }], nuevo: [{ id: 11 }] } }, resultado: 'EXITOSO',
      }),
    });

    expect(event.eventLabel).toBe('Proyecto actualizado');
    expect(event.observation).toBe('El proyecto se actualizó de la versión 4 a la 5. Campos modificados: Nombre, Unidades responsables.');
    expect(event.detailFields).toEqual(expect.arrayContaining([
      { label: 'Tipo de registro', value: 'Proyecto' },
      { label: 'Versión anterior', value: '4' },
      { label: 'Versión nueva', value: '5' },
      { label: 'Cambios', value: expect.stringContaining('Anterior: Proyecto anterior; Nuevo: Proyecto actualizado') },
    ]));
    expect(event.detailFields.find((field) => field.label === 'Cambios')?.value).not.toContain('[object Object]');
    expect(event.technicalDetail).toContain('"versionAnterior": 4');
  });

  it('resume una actualización de iniciativa sin cambios y humaniza claves camelCase desconocidas', () => {
    const event = presentAuditEvent({
      ...baseEvent,
      event: 'INICIATIVA_ACTUALIZADA',
      observation: '{"tipoRegistro":"Iniciativa","versionAnterior":7,"versionNueva":8,"cambios":{}}',
      rawDetail: '{"tipoRegistro":"Iniciativa","versionAnterior":7,"versionNueva":8,"cambios":{}}',
    });

    expect(event.eventLabel).toBe('Iniciativa actualizada');
    expect(event.observation).toBe('La iniciativa se actualizó de la versión 7 a la 8.');
    expect(event.detailFields).toContainEqual({ label: 'Tipo de registro', value: 'Iniciativa' });
    expect(event.detailFields.find((field) => field.label === 'Cambios')?.value).toBe('{}');
  });
});
