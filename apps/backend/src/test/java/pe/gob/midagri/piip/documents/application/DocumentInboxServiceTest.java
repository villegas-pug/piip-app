package pe.gob.midagri.piip.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.documents.application.DocumentInboxService.DossierSummary;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;

@ExtendWith(MockitoExtension.class)
class DocumentInboxServiceTest {
    @Mock PortfolioRecordRepository records;
    @Mock DocumentRepository documents;
    @Mock LocalAuthorizationService authorization;
    @Mock ResponsibleUnitRepository responsibleUnits;
    private PortfolioRecordEntity record; private DocumentTypeEntity type;
    private DocumentInboxService service;

    @BeforeEach
    void setUp() {
        service = new DocumentInboxService(records, documents, authorization, responsibleUnits);
        InstitutionEntity institution = new InstitutionEntity("I2", "Institución");
        ReflectionTestUtils.setField(institution, "id", 14L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE2", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 15L);
        record = PortfolioRecordTestBuilder.transientReferences().initiative("I-02", unit, "Iniciativa");
        ReflectionTestUtils.setField(record, "id", 17L);
        type = new DocumentTypeEntity("OPINION", "Informe de opinión", 20, true);
        ReflectionTestUtils.setField(type, "id", 19L);
    }

    @Test void cuentaPorTipoDocumentalEnLaEscalaDelCatalogoAunqueElTipoTengaVariosArchivos() {
        DocumentEntity opinionSlot = slot(20L);
        opinionSlot.registerUpload();
        DocumentFileEntity original = file(opinionSlot, 21L, true);
        DocumentFileEntity independent = file(opinionSlot, 22L, false);
        DocumentTypeEntity otherType = new DocumentTypeEntity("OTRO", "Otro tipo", 30, true);
        ReflectionTestUtils.setField(otherType, "id", 25L);
        DocumentEntity otherSlot = new DocumentEntity(record, otherType);
        ReflectionTestUtils.setField(otherSlot, "id", 26L);

        DossierSummary summary = resumen(opinionSlot, otherSlot);

        assertThat(summary.loadedCount()).isEqualTo(1);
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.notApplicableCount()).isZero();
        assertThat(original.isDeleted()).isFalse();
        assertThat(independent.isDeleted()).isFalse();
    }

    @Test void elTipoCuyosArchivosActivosSeEliminanCuentaComoPendiente() {
        DocumentEntity opinionSlot = slot(20L);
        opinionSlot.registerUpload();
        DocumentFileEntity original = file(opinionSlot, 21L, true);
        DocumentFileEntity independent = file(opinionSlot, 22L, false);
        original.markDeleted("subject");
        independent.markDeleted("subject");
        opinionSlot.markPending();

        DossierSummary summary = resumen(opinionSlot);

        assertThat(summary.loadedCount()).isZero();
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.notApplicableCount()).isZero();
    }

    @Test void elTipoConDeclaracionNoAplicaCuentaComoNoAplicaAunqueConvivanArchivos() {
        DocumentEntity opinionSlot = slot(20L);
        opinionSlot.registerUpload();
        file(opinionSlot, 21L, true);
        opinionSlot.markNotApplicable("No aplica en esta etapa");

        DossierSummary summary = resumen(opinionSlot);

        assertThat(summary.loadedCount()).isZero();
        assertThat(summary.pendingCount()).isZero();
        assertThat(summary.notApplicableCount()).isEqualTo(1);
    }

    private DossierSummary resumen(DocumentEntity... slots) {
        when(authorization.requireAuthenticatedRole())
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 14L, 15L))));
        when(records.findAll()).thenReturn(List.of(record));
        when(documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(17L)).thenReturn(List.of(slots));
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(17L)).thenReturn(List.of());
        return service.list().get(0);
    }
    private DocumentEntity slot(Long id) {
        DocumentEntity value = new DocumentEntity(record, type);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
    private DocumentFileEntity file(DocumentEntity document, Long id, boolean original) {
        DocumentFileEntity value = new DocumentFileEntity(document, original);
        ReflectionTestUtils.setField(value, "id", id);
        value.registerUpload();
        return value;
    }
}
