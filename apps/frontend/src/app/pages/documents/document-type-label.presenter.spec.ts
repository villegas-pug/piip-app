import { presentDocumentTypeLabel } from './document-type-label.presenter';

describe('presentDocumentTypeLabel', () => {
  it('normaliza los nombres documentales conocidos sin modificar sus códigos', () => {
    expect(presentDocumentTypeLabel('Ficha de Iniciativa de Innovacion Publica', 'PUBLIC_INNOVATION_INITIATIVE_SHEET'))
      .toBe('Ficha de Iniciativa de Innovación Pública');
    expect(presentDocumentTypeLabel('Documentacion de la gestion del proyecto'))
      .toBe('Documentación de la gestión del proyecto');
  });

  it('conserva un nombre histórico no reconocido', () => {
    expect(presentDocumentTypeLabel('Nombre histórico recibido del backend'))
      .toBe('Nombre histórico recibido del backend');
  });
});
