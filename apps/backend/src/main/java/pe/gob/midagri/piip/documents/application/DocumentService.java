package pe.gob.midagri.piip.documents.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.api.DocumentDtos.*;
import pe.gob.midagri.piip.documents.domain.*;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.catalogs.api.CatalogDtos.PersistentCatalogItemResponse;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;
import pe.gob.midagri.piip.work.persistence.*;
import java.io.IOException;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentService {
    private static final Set<String> ALLOWED_MIME = Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final PortfolioRecordRepository records; private final DocumentRepository documents;
    private final DocumentVersionRepository versions; private final DocumentContentRepository contents;
    private final UserRoleScopeRepository scopes; private final NotificationRepository notifications;
    private final LocalAuthorizationService authorization; private final AuditService audit;
    private final PiipProperties.Documents properties;
    private final DocumentTypeRepository documentTypes;

    public DocumentService(PortfolioRecordRepository records, DocumentRepository documents, DocumentVersionRepository versions,
            DocumentContentRepository contents, UserRoleScopeRepository scopes, NotificationRepository notifications,
            LocalAuthorizationService authorization, AuditService audit, PiipProperties.Documents properties, DocumentTypeRepository documentTypes) {
        this.records = records; this.documents = documents; this.versions = versions; this.contents = contents;
        this.scopes = scopes; this.notifications = notifications; this.authorization = authorization; this.audit = audit; this.properties = properties; this.documentTypes = documentTypes;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(String recordCode) {
        PortfolioRecordEntity record = record(recordCode);
        LocalAccessContext access = authorization.requireReadableUnit(record.getExecutingUnit().getId());
        boolean internal = access.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
            record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId());
        return documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(record.getId()).stream().map(document -> toResponse(document, internal)).toList();
    }

    @Transactional
    public VersionResponse upload(String recordCode, Long documentTypeId, DocumentUploadInput file) {
        PortfolioRecordEntity record = record(recordCode); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        validate(file); byte[] bytes = read(file);
        DocumentEntity document = documentSlot(record, documentTypeId);
        int number = document.registerUpload();
        DocumentVersionEntity version = versions.save(new DocumentVersionEntity(document, number, sanitize(file.originalFilename()), file.contentType(), bytes.length, sha256(bytes), actor.subject()));
        contents.save(new DocumentContentEntity(version, bytes));
        audit.event("DOCUMENTO_CARGADO", "REGISTRO_PORTAFOLIO", recordCode, documentAudit(document, number), actor.subject());
        return toVersion(version);
    }

    /** @deprecated la frontera HTTP debe usar {@link DocumentUploadInput}. */
    @Deprecated(forRemoval = false)
    public VersionResponse upload(String recordCode, Long documentTypeId, MultipartFile file) {
        return upload(recordCode, documentTypeId, pe.gob.midagri.piip.documents.api.MultipartDocumentUploadAdapter.adapt(file));
    }

    @Transactional
    public void markNotApplicable(String recordCode, Long documentTypeId, String reason) {
        PortfolioRecordEntity record = record(recordCode); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        DocumentEntity document = documentSlot(record, documentTypeId);
        document.markNotApplicable(reason);
        audit.event("DOCUMENTO_NO_APLICA", "REGISTRO_PORTAFOLIO", recordCode, Map.of("tipoCodigo", document.getType().getCode(), "tipoNombre", document.getType().getName(), "motivo", reason == null ? "" : reason), actor.subject());
    }

    @Transactional
    public VersionResponse publish(Long versionId, boolean published, long expectedVersion) {
        DocumentVersionEntity version = versions.findById(versionId).orElseThrow(() -> new NotFoundException("Versión documental inexistente"));
        PortfolioRecordEntity record = version.getDocument().getRecord(); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        if (version.getOptimisticVersion() != expectedVersion) throw new StaleVersionException();
        if (published) { version.publish(actor.subject()); notifyExternal(record, version); } else version.unpublish();
        audit.event(published ? "DOCUMENTO_PUBLICADO" : "DOCUMENTO_RETIRADO", "REGISTRO_PORTAFOLIO", record.getCode(),
            Map.of("versionId", versionId, "tipoCodigo", version.getDocument().getType().getCode(), "tipoNombre", version.getDocument().getType().getName()), actor.subject());
        return toVersion(version);
    }

    @Transactional(readOnly = true)
    public DownloadResponse download(Long versionId) {
        DocumentVersionEntity version = versions.findById(versionId).orElseThrow(() -> new NotFoundException("Versión documental inexistente"));
        PortfolioRecordEntity record = version.getDocument().getRecord(); LocalAccessContext access = authorization.requireReadableUnit(record.getExecutingUnit().getId());
        boolean internal = access.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
            record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId());
        if (!internal && !version.isExternallyPublished()) throw new AccessDeniedException("La versión no está publicada para consulta externa");
        byte[] content = contents.findByDocumentVersionId(versionId).orElseThrow(() -> new NotFoundException("Contenido documental inexistente")).getContent();
        return new DownloadResponse(version.getFilename(), version.getMimeType(), content);
    }

    private void notifyExternal(PortfolioRecordEntity record, DocumentVersionEntity version) {
        scopes.findActiveRecipients(RoleCode.CONSULTA_EXTERNA, record.getExecutingUnit().getInstitution().getId(), record.getExecutingUnit().getId(), Instant.now()).stream()
            .map(UserRoleScopeEntity::getUser).distinct()
            .forEach(user -> notifications.save(new NotificationEntity(user, record, "DOCUMENTO_PUBLICADO", "Se publicó " + version.getDocument().getType().getName() + " para " + record.getCode())));
    }

    private PortfolioRecordEntity record(String code) { return records.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Registro inexistente")); }
    private DocumentResponse toResponse(DocumentEntity document, boolean internal) {
        List<VersionResponse> items = versions.findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream().filter(item -> internal || item.isExternallyPublished()).map(this::toVersion).toList();
        var type = document.getType();
        return new DocumentResponse(document.getId(), new PersistentCatalogItemResponse(type.getId(), type.getCode(), type.getName(), type.getDisplayOrder(), type.isActive()), document.getState(), document.getNotApplicableReason(), document.getLatestVersion(), items);
    }
    private DocumentEntity documentSlot(PortfolioRecordEntity record, Long documentTypeId) {
        if (documentTypeId == null) throw new InvalidReferenceException("El tipo documental es obligatorio", "documentTypeId", null, "NOT_FOUND");
        return documents.findByRecordIdAndTypeId(record.getId(), documentTypeId).orElseGet(() -> {
            DocumentTypeEntity type = documentTypes.findById(documentTypeId)
                .orElseThrow(() -> new InvalidReferenceException("El tipo documental no existe", "documentTypeId", documentTypeId, "NOT_FOUND"));
            if (!type.isActive()) throw new InvalidReferenceException("El tipo documental está inactivo", "documentTypeId", documentTypeId, "INACTIVE");
            return documents.save(new DocumentEntity(record, type));
        });
    }
    private Map<String, ?> documentAudit(DocumentEntity document, int version) {
        return Map.of("tipoCodigo", document.getType().getCode(), "tipoNombre", document.getType().getName(), "version", version);
    }
    private VersionResponse toVersion(DocumentVersionEntity value) { return new VersionResponse(value.getId(), value.getVersionNumber(), value.getFilename(), value.getMimeType(), value.getSizeBytes(), value.getChecksumSha256(), value.getUploadedAt(), value.isExternallyPublished(), value.getOptimisticVersion()); }
    private void validate(DocumentUploadInput file) { if (file.sizeBytes() == 0) throw new BusinessRuleException("El archivo está vacío"); if (file.sizeBytes() > properties.maxSizeBytes()) throw new BusinessRuleException("El archivo excede el límite configurado"); if (!ALLOWED_MIME.contains(file.contentType())) throw new BusinessRuleException("Tipo MIME no permitido"); }
    private byte[] read(DocumentUploadInput file) { return file.bytes().get(); }
    private String sanitize(String name) { if (name == null || name.isBlank()) return "documento"; return name.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    private String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
}
