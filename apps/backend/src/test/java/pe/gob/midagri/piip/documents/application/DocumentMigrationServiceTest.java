package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@ExtendWith(MockitoExtension.class)
class DocumentMigrationServiceTest {
    @Mock DocumentFileRepository files;
    @Mock DocumentVersionRepository versions;
    private PortfolioRecordTestBuilder builder; private DocumentTypeEntity type;
    private ExecutingUnitEntity unit;
    private DocumentMigrationService service;

    @BeforeEach
    void setUp() {
        service = new DocumentMigrationService(files, versions);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        builder = PortfolioRecordTestBuilder.transientReferences();
        type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
    }

    @Test void migraPosicionConTresVersionesAUnArchivoOriginal() {
        DocumentEntity document = slot(initiative("I-02", 17L), 20L);
        DocumentVersionEntity first = legacyVersion(document, 1);
        DocumentVersionEntity second = legacyVersion(document, 2);
        DocumentVersionEntity third = legacyVersion(document, 3);
        second.publish("publicador");
        Instant secondUploadedAt = second.getUploadedAt();
        when(versions.findByFileIsNull()).thenReturn(List.of(first, second, third));
        AtomicReference<DocumentFileEntity> created = new AtomicReference<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L);
            created.set(value); return value;
        });

        DocumentMigrationReport report = service.migrate();

        assertThat(report.positions()).isEqualTo(1);
        assertThat(report.filesCreated()).isEqualTo(1);
        assertThat(report.versionsAssigned()).isEqualTo(3);
        assertThat(report.entries()).hasSize(1);
        assertThat(report.entries().get(0).recordCode()).isEqualTo("I-02");
        assertThat(report.entries().get(0).documentTypeCode()).isEqualTo("OPINION");
        assertThat(report.entries().get(0).documentTypeName()).isEqualTo("Informe de opinión");
        assertThat(report.entries().get(0).filesCreated()).isEqualTo(1);
        assertThat(report.entries().get(0).versionsAssigned()).isEqualTo(3);
        assertThat(created.get().isOriginal()).isTrue();
        assertThat(created.get().isDeleted()).isFalse();
        assertThat(created.get().getLatestVersion()).isEqualTo(3);
        assertThat(created.get().getDocument()).isSameAs(document);
        assertThat(first.getFile()).isSameAs(created.get());
        assertThat(second.getFile()).isSameAs(created.get());
        assertThat(third.getFile()).isSameAs(created.get());
        assertThat(first.getDocument()).isSameAs(document);
        assertThat(second.getFilename()).isEqualTo("informe-2.pdf");
        assertThat(second.getChecksumSha256()).isEqualTo("sha-2");
        assertThat(second.getSizeBytes()).isEqualTo(2L);
        assertThat(second.getUploadedBy()).isEqualTo("subject");
        assertThat(second.getUploadedAt()).isEqualTo(secondUploadedAt);
        assertThat(second.isExternallyPublished()).isTrue();
        assertThat(second.getOptimisticVersion()).isZero();
        verify(versions, never()).save(any());
    }

    @Test void posicionPendienteSinVersionesNoGeneraArchivo() {
        DocumentEntity document = slot(initiative("I-02", 17L), 20L);
        when(versions.findByFileIsNull()).thenReturn(List.of());

        DocumentMigrationReport report = service.migrate();

        assertThat(report.positions()).isZero();
        assertThat(report.filesCreated()).isZero();
        assertThat(report.versionsAssigned()).isZero();
        assertThat(report.entries()).isEmpty();
        verify(files, never()).save(any());
        assertThat(document.getState()).isEqualTo(DocumentState.PENDING);
    }

    @Test void conservaDeclaracionNoAplicaDeLaPosicion() {
        DocumentEntity document = slot(initiative("I-02", 17L), 20L);
        document.markNotApplicable("No aplica en esta etapa");
        DocumentVersionEntity only = legacyVersion(document, 1);
        when(versions.findByFileIsNull()).thenReturn(List.of(only));
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L); return value;
        });

        service.migrate();

        assertThat(document.getState()).isEqualTo(DocumentState.NOT_APPLICABLE);
        assertThat(document.getNotApplicableReason()).isEqualTo("No aplica en esta etapa");
        assertThat(only.getFile()).isNotNull();
    }

    @Test void reEjecutarLaMigracionNoCambiaNada() {
        DocumentEntity document = slot(initiative("I-02", 17L), 20L);
        DocumentVersionEntity first = legacyVersion(document, 1);
        DocumentVersionEntity second = legacyVersion(document, 2);
        when(versions.findByFileIsNull()).thenReturn(List.of(first, second), List.of());
        AtomicReference<DocumentFileEntity> created = new AtomicReference<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L);
            created.set(value); return value;
        });

        DocumentMigrationReport firstRun = service.migrate();
        DocumentMigrationReport secondRun = service.migrate();

        assertThat(firstRun.versionsAssigned()).isEqualTo(2);
        assertThat(secondRun.positions()).isZero();
        assertThat(secondRun.filesCreated()).isZero();
        assertThat(secondRun.versionsAssigned()).isZero();
        assertThat(secondRun.entries()).isEmpty();
        verify(files, times(1)).save(any(DocumentFileEntity.class));
        assertThat(first.getFile()).isSameAs(created.get());
        assertThat(second.getFile()).isSameAs(created.get());
    }

    @Test void migraIniciativaYProyectoDerivadoPorSeparadoSinMezcla() {
        PortfolioRecordEntity initiative = initiative("I-02", 17L);
        PortfolioRecordEntity project = builder.derivedProject("P-02", initiative, "Proyecto derivado");
        ReflectionTestUtils.setField(project, "id", 18L);
        DocumentEntity initiativeSlot = slot(initiative, 20L);
        DocumentEntity projectSlot = slot(project, 21L);
        DocumentVersionEntity initiativeVersion = legacyVersion(initiativeSlot, 1);
        DocumentVersionEntity projectVersion = legacyVersion(projectSlot, 1);
        when(versions.findByFileIsNull()).thenReturn(List.of(initiativeVersion, projectVersion));
        List<DocumentFileEntity> created = new ArrayList<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 40L + created.size());
            created.add(value); return value;
        });

        DocumentMigrationReport report = service.migrate();

        assertThat(created).hasSize(2);
        assertThat(created.get(0).getDocument()).isSameAs(initiativeSlot);
        assertThat(created.get(1).getDocument()).isSameAs(projectSlot);
        assertThat(initiativeVersion.getFile()).isSameAs(created.get(0));
        assertThat(initiativeVersion.getDocument()).isSameAs(initiativeSlot);
        assertThat(projectVersion.getFile()).isSameAs(created.get(1));
        assertThat(projectVersion.getDocument()).isSameAs(projectSlot);
        assertThat(report.positions()).isEqualTo(2);
        assertThat(report.filesCreated()).isEqualTo(2);
        assertThat(report.versionsAssigned()).isEqualTo(2);
        assertThat(report.entries()).extracting(DocumentMigrationReport.Entry::recordCode).containsExactlyInAnyOrder("I-02", "P-02");
    }

    @Test void elProyectoDerivadoSinVersionesNoHeredaArchivos() {
        PortfolioRecordEntity initiative = initiative("I-02", 17L);
        PortfolioRecordEntity project = builder.derivedProject("P-02", initiative, "Proyecto derivado");
        ReflectionTestUtils.setField(project, "id", 18L);
        DocumentEntity projectSlot = slot(project, 21L);
        DocumentEntity initiativeSlot = slot(initiative, 20L);
        DocumentVersionEntity only = legacyVersion(initiativeSlot, 1);
        when(versions.findByFileIsNull()).thenReturn(List.of(only));
        AtomicReference<DocumentFileEntity> created = new AtomicReference<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L);
            created.set(value); return value;
        });

        DocumentMigrationReport report = service.migrate();

        verify(files, times(1)).save(any(DocumentFileEntity.class));
        assertThat(created.get().getDocument()).isSameAs(initiativeSlot);
        assertThat(projectSlot.getState()).isEqualTo(DocumentState.PENDING);
        assertThat(report.entries()).hasSize(1);
        assertThat(report.entries().get(0).recordCode()).isEqualTo("I-02");
        assertThat(report.versionsAssigned()).isEqualTo(1);
    }

    private PortfolioRecordEntity initiative(String code, Long id) {
        PortfolioRecordEntity value = builder.initiative(code, unit, "Iniciativa");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
    private DocumentEntity slot(PortfolioRecordEntity record, Long id) {
        DocumentEntity value = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
    private DocumentVersionEntity legacyVersion(DocumentEntity document, int number) {
        DocumentVersionEntity value = new DocumentVersionEntity(new DocumentFileEntity(document, true), number,
            "informe-" + number + ".pdf", "application/pdf", number, "sha-" + number, "subject");
        ReflectionTestUtils.setField(value, "id", 30L + number);
        ReflectionTestUtils.setField(value, "file", null);
        return value;
    }
}
