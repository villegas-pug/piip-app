package pe.gob.midagri.piip.portfolio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.InstitutionRepository;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.catalogs.persistence.*;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.application.*;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.*;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioFlowPersistenceTest {
    @Autowired InstitutionRepository institutions;
    @Autowired ExecutingUnitRepository executingUnits;
    @Autowired PortfolioRecordRepository records;
    @Autowired CatalogRepository catalogs;
    @Autowired CatalogItemRepository catalogItems;

    @Test
    void preservesTheExistingRegistrationApprovalAndDerivationStates() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-FLOW-JPA", "Institución de flujo"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-FLOW-JPA", "Unidad de flujo"));
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, "flow");
        CatalogEntity peiCatalog = catalogs.save(new CatalogEntity(pe.gob.midagri.piip.catalogs.domain.CatalogCode.PEI_OBJECTIVE, "PEI", 30, true));
        CatalogEntity poiCatalog = catalogs.save(new CatalogEntity(pe.gob.midagri.piip.catalogs.domain.CatalogCode.POI_ACTIVITY, "POI", 40, true));
        CatalogItemEntity pei = catalogItems.save(new CatalogItemEntity(peiCatalog, "PEI-001", "Objetivo", 10, true));
        CatalogItemEntity poi = catalogItems.save(new CatalogItemEntity(poiCatalog, "POI-001", "Actividad", 10, true));
        PortfolioRecordEntity initiative = records.save(PortfolioRecordEntity.initiative("I-FLOW-JPA-2026", unit, "Iniciativa",
            fixtures.solution(), fixtures.source(), LocalDate.of(2026, 8, 18), "Responsable", pei, null,
            "Descripción", null, DigitalComponent.NO, "subject-flow"));

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.PRESENTED);
        initiative.approve();
        records.flush();

        PortfolioRecordEntity project = records.save(PortfolioRecordEntity.derivedProject("P-FLOW-JPA-2026", initiative, "Proyecto",
            fixtures.solution(), fixtures.source(), LocalDate.of(2026, 8, 18), "Responsable", null, poi,
            "Descripción", null, null, DigitalComponent.NO, "subject-flow"));
        records.flush();

        assertThat(initiative.getStatus()).isEqualTo(PortfolioStatus.INITIATIVE_APPROVED);
        assertThat(project.getStatus()).isEqualTo(PortfolioStatus.PROJECT_IN_PROGRESS);
        assertThat(project.getOriginRecord()).isSameAs(initiative);
        assertThat(initiative.getSolutionType().getId()).isEqualTo(fixtures.solution().getId());
        assertThat(initiative.getSourceOrigin().getId()).isEqualTo(fixtures.source().getId());
        assertThat(initiative.getPeiObjective().getId()).isEqualTo(pei.getId());
        assertThat(initiative.getPoiActivity()).isNull();
        assertThat(project.getPeiObjective()).isNull();
        assertThat(project.getPoiActivity().getId()).isEqualTo(poi.getId());

        CatalogItemEntity notApplicable = catalogItems.save(new CatalogItemEntity(fixtures.solution().getCatalog(),
            "NOT_APPLICABLE", "No aplica", 90, true));
        PortfolioRecordEntity preexisting = records.save(PortfolioRecordEntity.preexistingProject("P-PRE-JPA-2026", unit,
            "Proyecto preexistente", notApplicable, fixtures.source(), LocalDate.of(2026, 8, 18), "Responsable",
            null, poi, "Descripción", null, null, DigitalComponent.NO, "subject-flow"));
        records.flush();
        assertThat(preexisting.getSolutionType().getId()).isEqualTo(notApplicable.getId());
        assertThat(preexisting.getPeiObjective()).isNull();
        assertThat(preexisting.getPoiActivity().getId()).isEqualTo(poi.getId());
    }

    @Test
    void noGuardaCuandoUnaReferenciaEsInvalida() {
        PortfolioRecordRepository mockedRecords = mock(PortfolioRecordRepository.class);
        ExecutingUnitRepository mockedExecutingUnits = mock(ExecutingUnitRepository.class);
        CatalogReferenceService references = mock(CatalogReferenceService.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        CodeGeneratorService codes = mock(CodeGeneratorService.class);
        InstitutionEntity institution = new InstitutionEntity("MIDAGRI-ROLLBACK", "Institución rollback");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-ROLLBACK", "Unidad rollback");
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.transientReferences();
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 5L))
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        when(mockedExecutingUnits.findById(5L)).thenReturn(Optional.of(unit));
        when(codes.next(any(), anyInt())).thenReturn("I-ROLLBACK-2026");
        when(references.resolveActive(11L, CatalogCode.SOLUTION_TYPE, "solutionTypeId")).thenReturn(fixtures.solution());
        when(references.resolveActive(12L, CatalogCode.SOURCE_ORIGIN, "sourceId"))
            .thenThrow(new InvalidReferenceException("Referencia inválida", "sourceId", 12L, "INACTIVE"));
        InitiativeApplicationService service = new InitiativeApplicationService(mockedRecords, mock(ResponsibleUnitRepository.class), mockedExecutingUnits,
            mock(OrganizationalUnitRepository.class), mock(UserRepository.class), mock(WorkTaskRepository.class),
            mock(NotificationRepository.class), mock(DocumentRepository.class), codes, authorization, mock(AuditService.class),
            references, mock(DocumentTypeRepository.class));
        InitiativeCreateRequest request = new InitiativeCreateRequest(5L, "Iniciativa", 11L, 12L,
            LocalDate.of(2026, 8, 20), "Responsable", null, null, "Descripción", null, DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(8L)));

        assertThatThrownBy(() -> service.createInitiative(request)).isInstanceOf(InvalidReferenceException.class);
        verify(mockedRecords, never()).save(any(PortfolioRecordEntity.class));
    }

    @Test
    void enforcesOneDerivedProjectPerInitiativeAtPersistenceBoundary() {
        InstitutionEntity institution = institutions.save(new InstitutionEntity("MIDAGRI-UNIQUE-JPA", "Institución única"));
        ExecutingUnitEntity unit = executingUnits.save(new ExecutingUnitEntity(institution, "UE-UNIQUE-JPA", "Unidad única"));
        PortfolioRecordTestBuilder fixtures = PortfolioRecordTestBuilder.persistedReferences(catalogs, catalogItems, "unique");
        PortfolioRecordEntity initiative = records.save(fixtures.initiative("I-UNIQUE-JPA-2026", unit, "Iniciativa"));
        initiative.approve();
        records.saveAndFlush(fixtures.derivedProject("P-UNIQUE-JPA-2026", initiative, "Proyecto uno"));

        assertThatThrownBy(() -> records.saveAndFlush(fixtures.derivedProject("P-UNIQUE-JPA-2027", initiative, "Proyecto dos")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
