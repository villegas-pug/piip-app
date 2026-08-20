package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.documents.persistence.*;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.*;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.shared.api.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.persistence.*;

class ResponsibleUnitValidationTest {
    @Test void rechazaUnidadOrganicaInactivaAntesDeGuardarResponsabilidad() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        ExecutingUnitRepository executing = mock(ExecutingUnitRepository.class);
        OrganizationalUnitRepository organizational = mock(OrganizationalUnitRepository.class);
        CatalogReferenceService references = mock(CatalogReferenceService.class);
        InstitutionEntity institution = new InstitutionEntity("I", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 5L);
        OrganizationalUnitEntity inactive = new OrganizationalUnitEntity(unit, "UO", "Unidad inactiva", "UI");
        ReflectionTestUtils.setField(inactive, "id", 8L);
        ReflectionTestUtils.setField(inactive, "active", false);
        var fixture = PortfolioRecordTestBuilder.transientReferences();
        when(executing.findById(5L)).thenReturn(Optional.of(unit));
        when(organizational.findHistoricalById(8L)).thenReturn(Optional.of(inactive));
        when(references.resolveActive(eq(11L), eq(CatalogCode.SOLUTION_TYPE), anyString())).thenReturn(fixture.solution());
        when(references.resolveActive(eq(12L), eq(CatalogCode.SOURCE_ORIGIN), anyString())).thenReturn(fixture.source());
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        when(authorization.requireUnit(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP, 5L))
            .thenReturn(new LocalAccessContext(1L, "subject", Set.of()));
        CodeGeneratorService codes = mock(CodeGeneratorService.class);
        when(codes.next(any(), anyInt())).thenReturn("I-01");
        PortfolioService service = new PortfolioService(records, responsible, executing, organizational,
            mock(pe.gob.midagri.piip.identity.persistence.UserRepository.class), mock(WorkTaskRepository.class),
            mock(NotificationRepository.class), mock(DocumentRepository.class), codes, authorization, mock(AuditService.class),
            references, mock(DocumentTypeRepository.class));
        InitiativeCreateRequest request = new InitiativeCreateRequest(5L, "Iniciativa", 11L, 12L,
            LocalDate.of(2026, 8, 20), "Responsable", null, null, "Descripción", null, DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(8L)));

        assertThatThrownBy(() -> service.createInitiative(request)).isInstanceOf(InvalidReferenceException.class)
            .hasMessageContaining("inactiva");
        verifyNoInteractions(responsible);
    }

    @Test void rechazaUnidadOrganicaInexistenteSinGuardarResponsabilidades() {
        TestContext context = context();
        when(context.organizational().findHistoricalById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(context.service(), "saveResponsibleUnits",
            context.record(), List.of(new ResponsibleUnitInput(99L))))
            .isInstanceOf(InvalidReferenceException.class).hasMessage("La Unidad Orgánica no existe");
        verifyNoInteractions(context.responsible());
    }

    @Test void rechazaUnidadOrganicaDeOtraUnidadEjecutora() {
        TestContext context = context();
        InstitutionEntity institution = new InstitutionEntity("I-2", "Institución 2");
        ExecutingUnitEntity otherExecutingUnit = new ExecutingUnitEntity(institution, "UE-2", "Otra unidad");
        ReflectionTestUtils.setField(otherExecutingUnit, "id", 7L);
        OrganizationalUnitEntity other = new OrganizationalUnitEntity(otherExecutingUnit, "UO-2", "Unidad ajena", "UA");
        ReflectionTestUtils.setField(other, "id", 88L);
        when(context.organizational().findHistoricalById(88L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(context.service(), "saveResponsibleUnits",
            context.record(), List.of(new ResponsibleUnitInput(88L))))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessage("La Unidad Orgánica pertenece a otra Unidad Ejecutora");
        verifyNoInteractions(context.responsible());
    }

    @Test void guardaUnidadActivaDeLaMismaEjecutoraConSnapshotDerivado() {
        TestContext context = context();
        OrganizationalUnitEntity active = new OrganizationalUnitEntity(context.record().getExecutingUnit(), "UO-1", "Unidad válida", "UV");
        ReflectionTestUtils.setField(active, "id", 8L);
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(active));

        ReflectionTestUtils.invokeMethod(context.service(), "saveResponsibleUnits", context.record(), List.of(new ResponsibleUnitInput(8L)));

        verify(context.responsible()).save(argThat(saved -> saved.getOrganizationalUnit() == active
            && saved.getOriginalDesignation().equals("Unidad válida") && saved.getDisplayOrder() == 1));
    }

    @Test void lasCreacionesConResponsablesConservanFronteraTransaccional() throws NoSuchMethodException {
        Transactional transactional = PortfolioService.class
            .getMethod("createInitiative", InitiativeCreateRequest.class).getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
    }

    @Test void rechazaAmbitoAntesDeConsultarOGuardarUnidades() {
        TestContext context = context();
        when(context.authorization().requireUnit(pe.gob.midagri.piip.identity.domain.RoleCode.ADMINISTRADOR_PIIP, 5L))
            .thenThrow(new AccessDeniedException("fuera del ámbito"));
        InitiativeCreateRequest request = new InitiativeCreateRequest(5L, "Iniciativa", 11L, 12L,
            LocalDate.of(2026, 8, 20), "Responsable", null, null, "Descripción", null, DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(8L)));

        assertThatThrownBy(() -> context.service().createInitiative(request))
            .isInstanceOf(AccessDeniedException.class).hasMessageContaining("ámbito");
        verifyNoInteractions(context.executing(), context.organizational(), context.responsible());
    }

    private static TestContext context() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsible = mock(ResponsibleUnitRepository.class);
        ExecutingUnitRepository executing = mock(ExecutingUnitRepository.class);
        OrganizationalUnitRepository organizational = mock(OrganizationalUnitRepository.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        InstitutionEntity institution = new InstitutionEntity("I", "Institución");
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE", "Unidad");
        ReflectionTestUtils.setField(unit, "id", 5L);
        PortfolioRecordEntity record = PortfolioRecordTestBuilder.transientReferences().initiative("I-01", unit, "Iniciativa");
        PortfolioService service = new PortfolioService(records, responsible, executing, organizational,
            mock(pe.gob.midagri.piip.identity.persistence.UserRepository.class), mock(WorkTaskRepository.class),
            mock(NotificationRepository.class), mock(DocumentRepository.class), mock(CodeGeneratorService.class), authorization,
            mock(AuditService.class), mock(CatalogReferenceService.class), mock(DocumentTypeRepository.class));
        return new TestContext(service, record, responsible, organizational, executing, authorization);
    }

    private record TestContext(PortfolioService service, PortfolioRecordEntity record,
            ResponsibleUnitRepository responsible, OrganizationalUnitRepository organizational,
            ExecutingUnitRepository executing, LocalAuthorizationService authorization) {}
}
