package pe.gob.midagri.piip.catalogs.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.*;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.portfolio.domain.RecordType;

@Service
public class CatalogQueryService {
    private final CatalogItemRepository items;
    private final DocumentTypeRepository documentTypes;
    private final LocalAuthorizationService authorization;
    public CatalogQueryService(CatalogItemRepository items, DocumentTypeRepository documentTypes, LocalAuthorizationService authorization) {
        this.items = items; this.documentTypes = documentTypes; this.authorization = authorization;
    }
    @Transactional(readOnly = true)
    public CatalogBundleResponse bundle() {
        authorization.requireAuthenticatedRole();
        return new CatalogBundleResponse(
            Arrays.stream(RecordType.values()).map(value -> new TechnicalCatalogItemResponse(value.name(), value.label(), value.ordinal(), true)).toList(),
            persistent(CatalogCode.SOLUTION_TYPE), persistent(CatalogCode.SOURCE_ORIGIN), persistent(CatalogCode.PEI_OBJECTIVE), persistent(CatalogCode.POI_ACTIVITY),
            documentTypes.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream().map(this::response).toList());
    }
    private List<PersistentCatalogItemResponse> persistent(CatalogCode code) {
        return items.findByCatalogCodeAndCatalogActiveTrueAndActiveTrueOrderByDisplayOrderAscCodeAsc(code).stream().map(this::response).toList();
    }
    private PersistentCatalogItemResponse response(CatalogItemEntity value) { return new PersistentCatalogItemResponse(value.getId(), value.getCode(), value.getName(), value.getDisplayOrder(), value.isActive()); }
    private PersistentCatalogItemResponse response(DocumentTypeEntity value) { return new PersistentCatalogItemResponse(value.getId(), value.getCode(), value.getName(), value.getDisplayOrder(), value.isActive()); }
}
