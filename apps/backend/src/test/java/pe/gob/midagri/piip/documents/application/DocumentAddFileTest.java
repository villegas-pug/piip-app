package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.documents.api.DocumentDtos.FileResponse;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class DocumentAddFileTest {
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
    @Captor ArgumentCaptor<Map<String, ?>> auditDetail;
    private PortfolioRecordEntity record; private DocumentEntity slot;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(records, documents, files, versions, contents, scopes, notifications,
            authorization, audit, new PiipProperties.Documents(1024), documentTypes);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        DocumentTypeEntity type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
        slot = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(slot, "id", 20L);
    }

    @Test void dosArchivosSucesivosQuedanIndependientesConVersionUno() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(documents.findByRecordIdAndTypeId(17L, 19L)).thenReturn(Optional.of(slot));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        List<DocumentFileEntity> created = new ArrayList<>();
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L + created.size());
            created.add(value); return value;
        });
        List<DocumentVersionEntity> saved = new ArrayList<>();
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 30L + saved.size());
            saved.add(value); return value;
        });

        FileResponse first = service.addFile("I-02", 19L, upload("uno.pdf", new byte[] {1}));
        FileResponse second = service.addFile("I-02", 19L, upload("dos.pdf", new byte[] {2}));

        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(file -> { assertThat(file.isOriginal()).isFalse(); assertThat(file.getLatestVersion()).isEqualTo(1); });
        assertThat(saved).extracting(DocumentVersionEntity::getVersionNumber).containsExactly(1, 1);
        assertThat(saved.get(0).getFile()).isSameAs(created.get(0));
        assertThat(saved.get(1).getFile()).isSameAs(created.get(1));
        assertThat(saved.get(1).getFile()).isNotSameAs(created.get(0));
        assertThat(first.original()).isFalse(); assertThat(first.latestVersion()).isEqualTo(1);
        assertThat(first.current().version()).isEqualTo(1); assertThat(first.versions()).hasSize(1);
        assertThat(second.original()).isFalse(); assertThat(second.latestVersion()).isEqualTo(1);
        assertThat(slot.getState()).isEqualTo(DocumentState.LOADED);
        verify(files, never()).findFirstByDocumentIdAndOriginalTrueAndDeletedFalseOrderByIdAsc(anyLong());
        verify(contents, times(2)).save(any());
        verify(audit, times(2)).event(eq("DOCUMENTO_CARGADO"), eq("REGISTRO_PORTAFOLIO"), eq("I-02"), auditDetail.capture(), eq("subject"));
        assertThat(auditDetail.getAllValues().get(0).get("archivoId")).isEqualTo(21L);
        assertThat(auditDetail.getAllValues().get(1).get("archivoId")).isEqualTo(22L);
    }

    @Test void reanudaTipoConDeclaracionNoAplicaVigente() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(documents.findByRecordIdAndTypeId(17L, 19L)).thenReturn(Optional.of(slot));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(files.save(any(DocumentFileEntity.class))).thenAnswer(invocation -> {
            DocumentFileEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 21L); return value;
        });
        when(versions.save(any())).thenAnswer(invocation -> {
            DocumentVersionEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 30L); return value;
        });
        slot.markNotApplicable("No aplica en esta etapa");
        assertThat(slot.getState()).isEqualTo(DocumentState.NOT_APPLICABLE);

        FileResponse response = service.addFile("I-02", 19L, upload("informe.pdf", new byte[] {1}));

        assertThat(slot.getState()).isEqualTo(DocumentState.LOADED);
        assertThat(slot.getNotApplicableReason()).isEqualTo("No aplica en esta etapa");
        assertThat(response.latestVersion()).isEqualTo(1);
        verify(contents, times(1)).save(any());
    }

    @Test void rechazaArchivoVacioMimeNoPermitidoYTamanoSobreElMaximoSinPersistir() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));

        assertThatThrownBy(() -> service.addFile("I-02", 19L, upload("vacio.pdf", new byte[0])))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> service.addFile("I-02", 19L, new DocumentUploadInput("nota.txt", "text/plain", 1, () -> new byte[] {1})))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("MIME");
        assertThatThrownBy(() -> service.addFile("I-02", 19L, upload("grande.pdf", new byte[1025])))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("límite");

        verifyNoInteractions(documents, files, versions, contents, audit);
    }

    @Test void rechazaTipoInexistenteOInactivoComoReferenciaNueva() {
        DocumentTypeEntity inactive = new DocumentTypeEntity("OLD", "Tipo histórico", 10, false);
        ReflectionTestUtils.setField(inactive, "id", 10L);
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L)).thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(documents.findByRecordIdAndTypeId(17L, 9L)).thenReturn(Optional.empty());
        when(documents.findByRecordIdAndTypeId(17L, 10L)).thenReturn(Optional.empty());
        when(documentTypes.findById(9L)).thenReturn(Optional.empty());
        when(documentTypes.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.addFile("I-02", 9L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.addFile("I-02", 10L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("inactivo");

        verify(documents, never()).save(any());
        verify(files, never()).save(any());
    }

    @Test void rechazaEscrituraSinAdministracionDeLaUnidadEjecutora() {
        when(records.findByCodeIgnoreCase("I-02")).thenReturn(Optional.of(record));
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 15L))
            .thenThrow(new AccessDeniedException("La Unidad Ejecutora está fuera del ámbito autorizado para el rol ADMINISTRADOR_PIIP"));

        assertThatThrownBy(() -> service.addFile("I-02", 19L, upload("informe.pdf", new byte[] {1})))
            .isInstanceOf(AccessDeniedException.class).hasMessageContaining("fuera del ámbito");

        verifyNoInteractions(documents, files, versions, contents, audit);
    }

    private DocumentUploadInput upload(String filename, byte[] content) { return new DocumentUploadInput(filename, "application/pdf", content.length, () -> content); }
}
