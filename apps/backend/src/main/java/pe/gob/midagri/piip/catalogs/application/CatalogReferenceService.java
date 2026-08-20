package pe.gob.midagri.piip.catalogs.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.shared.api.InvalidReferenceException;

@Service
@Transactional(readOnly = true)
public class CatalogReferenceService {
    private final CatalogItemRepository items;
    public CatalogReferenceService(CatalogItemRepository items) { this.items = items; }

    public CatalogItemEntity resolveActive(Long id, CatalogCode expected, String field) {
        if (id == null) return null;
        CatalogItemEntity item = items.findById(id).orElseThrow(() -> invalid(field, id, "NOT_FOUND", "La referencia no existe"));
        if (item.getCatalog().getCode() != expected) throw invalid(field, id, "WRONG_CATALOG", "La referencia no pertenece al catálogo esperado");
        if (!item.isActive() || !item.getCatalog().isActive()) throw invalid(field, id, "INACTIVE", "La referencia está inactiva");
        return item;
    }

    public CatalogItemEntity resolveActiveByCode(CatalogCode catalog, String code, String field) {
        return items.findByCatalogCodeAndCatalogActiveTrueAndCodeIgnoreCaseAndActiveTrue(catalog, code)
            .orElseThrow(() -> invalid(field, null, "NOT_FOUND", "No existe una referencia activa con el código técnico requerido"));
    }

    private InvalidReferenceException invalid(String field, Long id, String reason, String message) {
        return new InvalidReferenceException(message, field, id, reason);
    }
}
