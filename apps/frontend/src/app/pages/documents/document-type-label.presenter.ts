const KNOWN_DOCUMENT_TYPE_LABELS: Record<string, string> = {
  PUBLIC_INNOVATION_INITIATIVE_SHEET: 'Ficha de Iniciativa de Innovación Pública',
  INITIATIVE_TECHNICAL_OPINION: 'Informe de opinión técnica de evaluación de iniciativa',
  FORMAL_APPROVAL_DECISION: 'Documento formal de decisión de aprobación',
  FINAL_PRODUCT_APPROVAL: 'Documento formal de aprobación de producto final',
  PROJECT_MANAGEMENT_DOCUMENTATION: 'Documentación de la gestión del proyecto',
  FINAL_CLOSURE_REPORT: 'Informe final de cierre',
  'Ficha de Iniciativa de Innovacion Publica': 'Ficha de Iniciativa de Innovación Pública',
  'Informe de opinion tecnica de evaluacion de iniciativa': 'Informe de opinión técnica de evaluación de iniciativa',
  'Documento formal de decision de aprobacion': 'Documento formal de decisión de aprobación',
  'Documento formal de aprobacion de producto final': 'Documento formal de aprobación de producto final',
  'Documentacion de la gestion del proyecto': 'Documentación de la gestión del proyecto',
};

/** Normaliza solo nombres documentales conocidos, sin alterar códigos ni valores históricos desconocidos. */
export function presentDocumentTypeLabel(name: string | null | undefined, code?: string | null): string {
  if (code && KNOWN_DOCUMENT_TYPE_LABELS[code]) return KNOWN_DOCUMENT_TYPE_LABELS[code];
  if (!name) return 'Tipo documental no registrado';
  return KNOWN_DOCUMENT_TYPE_LABELS[name] ?? name;
}
