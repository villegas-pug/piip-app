package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.persistence.DocumentContentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentVersionEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentVersionRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class DocumentAuthorizationTest {
    @Mock PortfolioRecordRepository records;
    @Mock DocumentRepository documents;
    @Mock DocumentVersionRepository versions;
    @Mock DocumentContentRepository contents;
    @Mock UserRoleScopeRepository scopes;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    @Mock PiipProperties.Documents properties;
    @Mock DocumentTypeRepository documentTypes;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(records, documents, versions, contents, scopes, notifications, authorization, audit, properties, documentTypes);
    }

    @Test
    void administratorInAnotherUnitCannotDownloadAnUnpublishedVersion() {
        PortfolioRecordEntity record = initiative("INI-UE1", 10L, 100L);
        DocumentEntity document = new DocumentEntity(record, new DocumentTypeEntity("INITIATIVE_TECHNICAL_OPINION", "Informe técnico", 20, true));
        DocumentVersionEntity version = new DocumentVersionEntity(document, 1, "informe.pdf", "application/pdf", 1L,
            "checksum", "subject");
        ReflectionTestUtils.setField(version, "id", 1L);
        LocalAccessContext actor = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(versions.findById(1L)).thenReturn(Optional.of(version));
        when(authorization.requireReadableUnit(100L)).thenReturn(actor);

        assertThatThrownBy(() -> service.download(1L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("no está publicada");
        verifyNoInteractions(contents);
    }

    private PortfolioRecordEntity initiative(String code, Long institutionId, Long unitId) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + institutionId, "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + unitId, "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        return PortfolioRecordTestBuilder.transientReferences().initiative(code, unit, "Iniciativa");
    }
}
