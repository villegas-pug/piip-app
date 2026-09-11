import { AuditEvent, PortfolioStatusReference } from '../../core/piip.models';
import { presentDocumentTypeLabel } from '../documents/document-type-label.presenter';

export interface AuditDetailField {
  label: string;
  value: string;
}

export interface PresentedAuditEvent {
  source: AuditEvent;
  eventLabel: string;
  observation: string;
  documentName?: string;
  technicalDetail: string;
  detailFields: AuditDetailField[];
  status?: PortfolioStatusReference;
  previousStatus?: PortfolioStatusReference;
  newStatus?: PortfolioStatusReference;
}

const EVENT_LABELS: Record<string, string> = {
  DOCUMENTO_CARGADO: 'Documento cargado',
  DOCUMENTO_ARCHIVO_ELIMINADO: 'Archivo de documento eliminado',
  DOCUMENTO_NO_APLICA: 'Documento marcado como No aplica',
  DOCUMENTO_PUBLICADO: 'Documento publicado',
  DOCUMENTO_RETIRADO: 'Publicación retirada',
  INICIATIVA_REGISTRADA: 'Iniciativa registrada',
  INICIATIVA_APROBADA: 'Iniciativa aprobada',
  ESTADO_INICIATIVA_CAMBIADO: 'Estado de iniciativa cambiado',
  ESTADO_PROYECTO_CAMBIADO: 'Estado de proyecto cambiado',
  PROYECTO_DERIVADO_REGISTRADO: 'Proyecto derivado registrado',
  PROYECTO_PREEXISTENTE_REGISTRADO: 'Proyecto preexistente registrado',
  TAREA_CREADA: 'Tarea creada',
  TAREA_COMPLETADA: 'Tarea completada',
  TAREA_REASIGNADA: 'Tarea reasignada',
  ROL_ASIGNADO: 'Rol asignado',
  ROL_SUSPENDIDO: 'Rol suspendido',
  INICIATIVA_ACTUALIZADA: 'Iniciativa actualizada',
  PROYECTO_ACTUALIZADO: 'Proyecto actualizado',
};

const DETAIL_LABELS: Record<string, string> = {
  tipo: 'Tipo documental', tipoCodigo: 'Código de tipo documental', tipoNombre: 'Tipo documental', version: 'Versión', versionId: 'Versión', versionVigente: 'Versión vigente', archivoId: 'Id de archivo', nombreVigente: 'Nombre de archivo vigente', estado: 'Estado', estadoAnterior: 'Estado anterior', estadoNuevo: 'Estado nuevo', observacion: 'Observación',
  registro: 'Expediente', motivo: 'Motivo', iniciativaOrigen: 'Iniciativa de origen', origen: 'Origen',
  asignadoA: 'Asignado a', rol: 'Rol', institucion: 'Institución', unidadEjecutora: 'Unidad Ejecutora', unidadEjecutoraId: 'Unidad Ejecutora', resultado: 'Resultado',
  tipoRegistro: 'Tipo de registro', versionAnterior: 'Versión anterior', versionNueva: 'Versión nueva', cambios: 'Cambios', anterior: 'Anterior', nuevo: 'Nuevo',
  name: 'Nombre', solutionType: 'Tipo de solución', source: 'Fuente u origen', startDate: 'Fecha de inicio', responsible: 'Responsable',
  peiObjective: 'Objetivo PEI', poiActivity: 'Actividad POI', responsibleUnits: 'Unidades Orgánicas Involucradas', description: 'Descripción', keyResults: 'Resultados clave', note: 'Nota', digitalComponent: 'Componente digital',
  nro: 'Nro', displayOrder: 'Nro', code: 'Código', sigla: 'Abreviatura',
};

type AuditDetail = Record<string, unknown>;

export function presentAuditEvent(event: AuditEvent): PresentedAuditEvent {
  const rawDetail = event.rawDetail ?? event.observation;
  const detail = parseDetail(rawDetail);
  return {
    source: event,
    eventLabel: EVENT_LABELS[event.event] ?? humanize(event.event),
    observation: summarize(event, detail),
    documentName: event.documentName ?? stringDetail(detail['nombreVigente']),
    technicalDetail: formatTechnicalDetail(rawDetail),
    status: event.status,
    previousStatus: event.previousStatus,
    newStatus: event.newStatus,
    detailFields: Object.entries(detail)
      .filter(([key]) => key !== 'tipoCodigo' && key !== 'statusCode' && key !== 'previousStatusCode' && key !== 'newStatusCode')
      .map(([key, value]) => ({ label: detailLabel(event.event, key), value: presentValue(key, value) })),
  };
}

function detailLabel(event: string, key: string): string {
  if (event === 'DOCUMENTO_ARCHIVO_ELIMINADO') {
    if (key === 'versionVigente') return 'Versión al momento de la eliminación';
    if (key === 'nombreVigente') return 'Nombre del archivo eliminado';
  }
  return DETAIL_LABELS[key] ?? humanize(key);
}

function stringDetail(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value : undefined;
}

function summarize(event: AuditEvent, detail: AuditDetail): string {
  const documentType = presentDocumentTypeLabel(
    presentValue('tipoNombre', detail['tipoNombre'] ?? detail['tipo']),
    typeof detail['tipoCodigo'] === 'string' ? detail['tipoCodigo'] : undefined,
  );
  const version = detail['version'] ?? detail['versionId'] ?? detail['versionVigente'];
  const record = presentValue('registro', detail['registro']);
  switch (event.event) {
    case 'DOCUMENTO_CARGADO': return `Se cargó ${documentType}${version == null ? '' : `, versión ${version}`}.`;
    case 'DOCUMENTO_ARCHIVO_ELIMINADO': return `Se eliminó ${documentType}${detail['nombreVigente'] ? ` (${detail['nombreVigente']})` : ''}${version == null ? '' : `, versión ${version}`}.`;
    case 'DOCUMENTO_NO_APLICA': return `Se marcó como No aplica ${documentType}${detail['motivo'] ? `. Motivo: ${detail['motivo']}` : ''}.`;
    case 'DOCUMENTO_PUBLICADO': return `Se publicó el documento${version == null ? '' : `, versión ${version}`}.`;
    case 'DOCUMENTO_RETIRADO': return `Se retiró la publicación del documento${version == null ? '' : `, versión ${version}`}.`;
    case 'INICIATIVA_REGISTRADA': return event.status?.name ? `Estado inicial: ${event.status.name}.` : (detail['estado'] ? `Estado inicial: ${presentValue('estado', detail['estado'])}.` : 'Se registró la iniciativa.');
    case 'INICIATIVA_APROBADA': return detail['observacion'] ? `Observación: ${detail['observacion']}` : 'La iniciativa fue aprobada.';
    case 'ESTADO_INICIATIVA_CAMBIADO': return statusChangeSummary('La iniciativa', event, detail);
    case 'ESTADO_PROYECTO_CAMBIADO': return statusChangeSummary('El proyecto', event, detail);
    case 'INICIATIVA_ACTUALIZADA': return updateSummary('La iniciativa', detail);
    case 'PROYECTO_ACTUALIZADO': return updateSummary('El proyecto', detail);
    case 'PROYECTO_DERIVADO_REGISTRADO': return detail['iniciativaOrigen'] ? `Iniciativa de origen: ${detail['iniciativaOrigen']}.` : 'Se registró el proyecto derivado.';
    case 'PROYECTO_PREEXISTENTE_REGISTRADO': return detail['origen'] ? `Origen: ${detail['origen']}.` : 'Se registró el proyecto preexistente.';
    case 'TAREA_CREADA': return record ? `Se creó una tarea para el expediente ${record}.` : 'Se creó una tarea.';
    case 'TAREA_COMPLETADA': return record ? `Se completó una tarea del expediente ${record}.` : 'Se completó una tarea.';
    case 'TAREA_REASIGNADA': return record ? `Se reasignó una tarea del expediente ${record}.` : 'Se reasignó una tarea.';
    case 'ROL_ASIGNADO': return `Rol ${detail['rol'] ?? 'registrado'} asignado${detail['unidadEjecutora'] ? ` para ${detail['unidadEjecutora']}` : ''}.`;
    case 'ROL_SUSPENDIDO': return `Rol ${detail['rol'] ?? 'registrado'} suspendido.`;
    default: return 'Evento registrado.';
  }
}

function updateSummary(subject: string, detail: AuditDetail): string {
  const previous = detail['versionAnterior'];
  const current = detail['versionNueva'];
  const version = previous == null || current == null
    ? ''
    : ` de la versión ${presentValue('versionAnterior', previous)} a la ${presentValue('versionNueva', current)}`;
  const changes = detail['cambios'];
  const fields = changes && typeof changes === 'object' && !Array.isArray(changes)
    ? Object.keys(changes).map((key) => DETAIL_LABELS[key] ?? humanize(key))
    : [];
  return fields.length
    ? `${subject} se actualizó${version}. Campos modificados: ${fields.join(', ')}.`
    : `${subject} se actualizó${version}.`;
}

function statusChangeSummary(subject: string, event: AuditEvent, detail: AuditDetail): string {
  const previous = event.previousStatus?.name ?? presentValue('estadoAnterior', detail['estadoAnterior']);
  const current = event.newStatus?.name ?? presentValue('estadoNuevo', detail['estadoNuevo']);
  const observation = detail['observacion'] ? ` Observación: ${detail['observacion']}` : '';
  return `${subject} cambió de ${previous} a ${current}.${observation}`;
}

function parseDetail(value: string): AuditDetail {
  try {
    const parsed: unknown = JSON.parse(value);
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as AuditDetail : {};
  } catch {
    return {};
  }
}

function formatTechnicalDetail(value: string): string {
  try { return JSON.stringify(JSON.parse(value), null, 2); }
  catch { return value || 'Sin datos técnicos registrados.'; }
}

function presentValue(key: string, value: unknown): string {
  if (value == null || value === '') return 'No registrado';
  if (Array.isArray(value)) return value.map((item) => presentValue('', item)).join(', ');
  if (typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>)
      .map(([entryKey, entryValue]) => `${DETAIL_LABELS[entryKey] ?? humanize(entryKey)}: ${presentValue(entryKey, entryValue)}`)
      .join('; ');
    return entries || '{}';
  }
  return key === 'tipoNombre' || key === 'tipo'
    ? presentDocumentTypeLabel(String(value))
    : String(value);
}

function humanize(value: string): string {
  return value
    .replace(/([a-záéíóúñ])([A-Z])/g, '$1 $2')
    .toLocaleLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toLocaleUpperCase());
}
