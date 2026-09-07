package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.api.DocumentDtos.DocumentResponse;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class DocumentFileListingTest {
    @Mock PortfolioRecordRepository records;
    @Mock DocumentRepository documents;
    @Mock DocumentFileRepository files;
    @Mock DocumentVersionRepository versions;
    @Mock DocumentContentRepository contents;
    @Mock UserRoleScopeRepository scopes;
    @Mock NotificationRepository notifications;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;
    @Mock DocumentTypeRepository documentTypes;
    private PortfolioRecordEntity record;
    private DocumentEntity slot;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(records, documents, files, versions, contents, scopes, notifications,
            authorization, audit, new PiipProperties.Documents(1024), documentTypes);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        ReflectionTestUtils.setField(institution, "id", 14L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        DocumentTypeEntity type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
        slot = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(slot, "id", 20L);
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(17L)).thenReturn(List.of(slot));
    }

    @Test
    void ordenaArchivosPorPrimeraCargaAntesQuePorIdentificador() {
        DocumentFileEntity firstIdButLater = file(21L, false, 1);
        DocumentFileEntity secondIdButEarlier = file(22L, true, 1);
        DocumentVersionEntity late = version(firstIdButLater, 31L, 1, "tarde.pdf", Instant.parse("2026-01-02T00:00:00Z"));
        DocumentVersionEntity early = version(secondIdButEarlier, 32L, 1, "temprano.pdf", Instant.parse("2026-01-01T00:00:00Z"));
        stubListing(internal(), List.of(late, early), List.of(firstIdButLater, secondIdButEarlier));

        List<DocumentResponse> response = service.list("I-02");

        assertThat(response.get(0).files()).extracting(item -> item.id()).containsExactly(22L, 21L);
    }

    @Test
    void listaArchivosIndependientesParaInternoConDosConsultasPorPosicion() {
        DocumentFileEntity original = file(21L, true, 2);
        DocumentFileEntity independent = file(22L, false, 1);
        DocumentVersionEntity a2 = version(original, 32L, 2, "a2.pdf", Instant.parse("2026-01-03T00:00:00Z"));
        DocumentVersionEntity a1 = version(original, 31L, 1, "a1.pdf", Instant.parse("2026-01-01T00:00:00Z"));
        DocumentVersionEntity b1 = version(independent, 33L, 1, "b1.pdf", Instant.parse("2026-01-02T00:00:00Z"));
        stubListing(internal(), List.of(a2, b1, a1), List.of(original, independent));

        DocumentResponse response = service.list("I-02").get(0);

        assertThat(response.files()).extracting(item -> item.id()).containsExactly(21L, 22L);
        assertThat(response.files().get(0).versions()).extracting(item -> item.version()).containsExactly(2, 1);
        assertThat(response.files().get(1).versions()).extracting(item -> item.version()).containsExactly(1);
        assertThat(response.versions()).extracting(item -> item.version()).containsExactly(2, 1);
        assertThat(response.latestVersion()).isEqualTo(2);
        verify(versions, times(1)).findByDocumentIdOrderByVersionNumberDesc(20L);
        verify(files, times(1)).findByDocumentIdAndDeletedFalseOrderByIdAsc(20L);
    }

    @Test
    void consultaExternaConservaArchivosSinVersionesPublicadas() {
        DocumentFileEntity original = file(21L, true, 2);
        DocumentFileEntity independent = file(22L, false, 1);
        DocumentVersionEntity a2 = version(original, 32L, 2, "a2.pdf", Instant.parse("2026-01-03T00:00:00Z"));
        DocumentVersionEntity a1 = version(original, 31L, 1, "a1.pdf", Instant.parse("2026-01-01T00:00:00Z"));
        a1.publish("subject");
        DocumentVersionEntity b1 = version(independent, 33L, 1, "b1.pdf", Instant.parse("2026-01-02T00:00:00Z"));
        stubListing(external(), List.of(a2, b1, a1), List.of(original, independent));

        DocumentResponse response = service.list("I-02").get(0);

        assertThat(response.files()).hasSize(2);
        assertThat(response.files().get(0).versions()).extracting(item -> item.version()).containsExactly(1);
        assertThat(response.files().get(0).current().version()).isEqualTo(1);
        assertThat(response.files().get(1).versions()).isEmpty();
        assertThat(response.files().get(1).current()).isNull();
    }

    @Test
    void mantieneCompatibilidadVaciaSinOriginalActivo() {
        DocumentFileEntity deletedOriginal = file(21L, true, 1);
        deletedOriginal.markDeleted("subject");
        DocumentFileEntity independent = file(22L, false, 1);
        DocumentVersionEntity originalVersion = version(deletedOriginal, 31L, 1, "original.pdf", Instant.parse("2026-01-01T00:00:00Z"));
        DocumentVersionEntity remainingVersion = version(independent, 32L, 1, "restante.pdf", Instant.parse("2026-01-02T00:00:00Z"));
        stubListing(internal(), List.of(remainingVersion, originalVersion), List.of(independent));

        DocumentResponse response = service.list("I-02").get(0);

        assertThat(response.latestVersion()).isZero();
        assertThat(response.versions()).isEmpty();
        assertThat(response.files()).extracting(item -> item.id()).containsExactly(22L);
    }

    private void stubListing(LocalAccessContext access, List<DocumentVersionEntity> history, List<DocumentFileEntity> active) {
        when(authorization.requireReadableUnit(15L)).thenReturn(access);
        when(versions.findByDocumentIdOrderByVersionNumberDesc(20L)).thenReturn(history);
        when(files.findByDocumentIdAndDeletedFalseOrderByIdAsc(20L)).thenReturn(active);
    }
    private LocalAccessContext internal() { return new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 14L, 15L))); }
    private LocalAccessContext external() { return new LocalAccessContext(2L, "externo", Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 14L, 15L))); }
    private DocumentFileEntity file(Long id, boolean original, int latestVersion) {
        DocumentFileEntity value = new DocumentFileEntity(slot, original, latestVersion);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
    private DocumentVersionEntity version(DocumentFileEntity file, Long id, int number, String filename, Instant uploadedAt) {
        DocumentVersionEntity value = new DocumentVersionEntity(file, number, filename, "application/pdf", 1L, "sha-" + id, "subject");
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "uploadedAt", uploadedAt);
        return value;
    }
}
