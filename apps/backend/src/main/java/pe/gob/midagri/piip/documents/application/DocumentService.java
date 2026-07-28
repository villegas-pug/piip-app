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
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.shared.api.*;
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

    public DocumentService(PortfolioRecordRepository records, DocumentRepository documents, DocumentVersionRepository versions,
            DocumentContentRepository contents, UserRoleScopeRepository scopes, NotificationRepository notifications,
            LocalAuthorizationService authorization, AuditService audit, PiipProperties.Documents properties) {
        this.records = records; this.documents = documents; this.versions = versions; this.contents = contents;
        this.scopes = scopes; this.notifications = notifications; this.authorization = authorization; this.audit = audit; this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(String recordCode) {
        PortfolioRecordEntity record = record(recordCode); authorization.requireReadableUnit(record.getExecutingUnit().getId());
        LocalAccessContext access = authorization.current();
        return documents.findByRecordIdOrderByType(record.getId()).stream().map(document -> toResponse(document, access.hasRole(RoleCode.ADMINISTRADOR_PIIP))).toList();
    }

    @Transactional
    public VersionResponse upload(String recordCode, DocumentType type, MultipartFile file) {
        PortfolioRecordEntity record = record(recordCode); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        validate(file); byte[] bytes = read(file);
        DocumentEntity document = documents.findByRecordIdAndType(record.getId(), type).orElseGet(() -> documents.save(new DocumentEntity(record, type)));
        int number = document.registerUpload();
        DocumentVersionEntity version = versions.save(new DocumentVersionEntity(document, number, sanitize(file.getOriginalFilename()), file.getContentType(), bytes.length, sha256(bytes), actor.subject()));
        contents.save(new DocumentContentEntity(version, bytes));
        audit.event("DOCUMENTO_CARGADO", "REGISTRO_PORTAFOLIO", recordCode, Map.of("tipo", type.name(), "version", number), actor.subject());
        return toVersion(version);
    }

    @Transactional
    public void markNotApplicable(String recordCode, DocumentType type, String reason) {
        PortfolioRecordEntity record = record(recordCode); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        DocumentEntity document = documents.findByRecordIdAndType(record.getId(), type).orElseGet(() -> documents.save(new DocumentEntity(record, type)));
        document.markNotApplicable(reason);
        audit.event("DOCUMENTO_NO_APLICA", "REGISTRO_PORTAFOLIO", recordCode, Map.of("tipo", type.name(), "motivo", reason == null ? "" : reason), actor.subject());
    }

    @Transactional
    public VersionResponse publish(Long versionId, boolean published, long expectedVersion) {
        DocumentVersionEntity version = versions.findById(versionId).orElseThrow(() -> new NotFoundException("Versión documental inexistente"));
        PortfolioRecordEntity record = version.getDocument().getRecord(); LocalAccessContext actor = authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, record.getExecutingUnit().getId());
        if (version.getOptimisticVersion() != expectedVersion) throw new StaleVersionException();
        if (published) { version.publish(actor.subject()); notifyExternal(record, version); } else version.unpublish();
        audit.event(published ? "DOCUMENTO_PUBLICADO" : "DOCUMENTO_RETIRADO", "REGISTRO_PORTAFOLIO", record.getCode(), Map.of("versionId", versionId), actor.subject());
        return toVersion(version);
    }

    @Transactional(readOnly = true)
    public DownloadResponse download(Long versionId) {
        DocumentVersionEntity version = versions.findById(versionId).orElseThrow(() -> new NotFoundException("Versión documental inexistente"));
        PortfolioRecordEntity record = version.getDocument().getRecord(); LocalAccessContext access = authorization.requireReadableUnit(record.getExecutingUnit().getId());
        if (access.hasRole(RoleCode.CONSULTA_EXTERNA) && !access.hasRole(RoleCode.ADMINISTRADOR_PIIP) && !version.isExternallyPublished()) throw new AccessDeniedException("La versión no está publicada para consulta externa");
        byte[] content = contents.findByDocumentVersionId(versionId).orElseThrow(() -> new NotFoundException("Contenido documental inexistente")).getContent();
        return new DownloadResponse(version.getFilename(), version.getMimeType(), content);
    }

    private void notifyExternal(PortfolioRecordEntity record, DocumentVersionEntity version) {
        scopes.findActiveRecipients(RoleCode.CONSULTA_EXTERNA, record.getExecutingUnit().getInstitution().getId(), record.getExecutingUnit().getId(), Instant.now()).stream()
            .map(UserRoleScopeEntity::getUser).distinct()
            .forEach(user -> notifications.save(new NotificationEntity(user, record, "DOCUMENTO_PUBLICADO", "Se publicó " + version.getDocument().getType().label() + " para " + record.getCode())));
    }

    private PortfolioRecordEntity record(String code) { return records.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Registro inexistente")); }
    private DocumentResponse toResponse(DocumentEntity document, boolean internal) {
        List<VersionResponse> items = versions.findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream().filter(item -> internal || item.isExternallyPublished()).map(this::toVersion).toList();
        return new DocumentResponse(document.getId(), document.getType(), document.getType().label(), document.getState(), document.getNotApplicableReason(), document.getLatestVersion(), items);
    }
    private VersionResponse toVersion(DocumentVersionEntity value) { return new VersionResponse(value.getId(), value.getVersionNumber(), value.getFilename(), value.getMimeType(), value.getSizeBytes(), value.getChecksumSha256(), value.getUploadedAt(), value.isExternallyPublished(), value.getOptimisticVersion()); }
    private void validate(MultipartFile file) { if (file.isEmpty()) throw new BusinessRuleException("El archivo está vacío"); if (file.getSize() > properties.maxSizeBytes()) throw new BusinessRuleException("El archivo excede el límite configurado"); if (!ALLOWED_MIME.contains(file.getContentType())) throw new BusinessRuleException("Tipo MIME no permitido"); }
    private byte[] read(MultipartFile file) { try { return file.getBytes(); } catch (IOException exception) { throw new BusinessRuleException("No se pudo leer el archivo"); } }
    private String sanitize(String name) { if (name == null || name.isBlank()) return "documento"; return name.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    private String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
}
