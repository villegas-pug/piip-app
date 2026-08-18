import { AuditEvent, DocumentType } from '../../core/piip.models';

export interface AuditDetailField {
  label: string;
  value: string;
}

export interface PresentedAuditEvent {
  source: AuditEvent;
  eventLabel: string;
  observation: string;
  technicalDetail: string;
  detailFields: AuditDetailField[];
}

const EVENT_LABELS: Record<string, string> = {
  DOCUMENTO_CARGADO: 'Documento cargado',
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
};

const DETAIL_LABELS: Record<string, string> = {
  tipo: 'Tipo documental', version: 'Versión', versionId: 'Versión', estado: 'Estado', estadoAnterior: 'Estado anterior', estadoNuevo: 'Estado nuevo', observacion: 'Observación',
  registro: 'Expediente', motivo: 'Motivo', iniciativaOrigen: 'Iniciativa de origen', origen: 'Origen',
  asignadoA: 'Asignado a', rol: 'Rol', institucion: 'Institución', unidadEjecutora: 'Unidad Ejecutora', unidadEjecutoraId: 'Unidad Ejecutora', resultado: 'Resultado',
};

const DOCUMENT_LABELS: Record<DocumentType, string> = {
  PUBLIC_INNOVATION_INITIATIVE_SHEET: 'Ficha de Iniciativa de Innovación Pública',
  INITIATIVE_TECHNICAL_OPINION: 'Informe de opinión técnica de evaluación de iniciativa',
  FORMAL_APPROVAL_DECISION: 'Documento formal de decisión de aprobación',
  FINAL_PRODUCT_APPROVAL: 'Documento formal de aprobación de producto final',
  PROJECT_MANAGEMENT_DOCUMENTATION: 'Documentación de la gestión del proyecto',
  FINAL_CLOSURE_REPORT: 'Informe final de cierre',
};

type AuditDetail = Record<string, unknown>;

export function presentAuditEvent(event: AuditEvent): PresentedAuditEvent {
  const rawDetail = event.rawDetail ?? event.observation;
  const detail = parseDetail(rawDetail);
  return {
    source: event,
    eventLabel: EVENT_LABELS[event.event] ?? humanize(event.event),
    observation: summarize(event.event, detail),
    technicalDetail: formatTechnicalDetail(rawDetail),
    detailFields: Object.entries(detail).map(([key, value]) => ({ label: DETAIL_LABELS[key] ?? humanize(key), value: presentValue(key, value) })),
  };
}

function summarize(event: string, detail: AuditDetail): string {
  const documentType = presentValue('tipo', detail['tipo']);
  const version = detail['version'] ?? detail['versionId'];
  const record = presentValue('registro', detail['registro']);
  switch (event) {
    case 'DOCUMENTO_CARGADO': return `Se cargó ${documentType}${version == null ? '' : `, versión ${version}`}.`;
    case 'DOCUMENTO_NO_APLICA': return `Se marcó como No aplica ${documentType}${detail['motivo'] ? `. Motivo: ${detail['motivo']}` : ''}.`;
    case 'DOCUMENTO_PUBLICADO': return `Se publicó el documento${version == null ? '' : `, versión ${version}`}.`;
    case 'DOCUMENTO_RETIRADO': return `Se retiró la publicación del documento${version == null ? '' : `, versión ${version}`}.`;
    case 'INICIATIVA_REGISTRADA': return detail['estado'] ? `Estado inicial: ${detail['estado']}.` : 'Se registró la iniciativa.';
    case 'INICIATIVA_APROBADA': return detail['observacion'] ? `Observación: ${detail['observacion']}` : 'La iniciativa fue aprobada.';
    case 'ESTADO_INICIATIVA_CAMBIADO': return statusChangeSummary('La iniciativa', detail);
    case 'ESTADO_PROYECTO_CAMBIADO': return statusChangeSummary('El proyecto', detail);
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

function statusChangeSummary(subject: string, detail: AuditDetail): string {
  const previous = presentValue('estadoAnterior', detail['estadoAnterior']);
  const current = presentValue('estadoNuevo', detail['estadoNuevo']);
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
  if (key === 'tipo' && typeof value === 'string' && value in DOCUMENT_LABELS) return DOCUMENT_LABELS[value as DocumentType];
  return String(value);
}

function humanize(value: string): string {
  return value.toLocaleLowerCase().replace(/[_-]+/g, ' ').replace(/\b\w/g, (letter) => letter.toLocaleUpperCase());
}
