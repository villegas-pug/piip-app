package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
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
        InitiativeApplicationService service = new InitiativeApplicationService(records, responsible, executing, organizational,
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

        assertThatThrownBy(() -> context.responsibleService().save(context.record(), List.of(new ResponsibleUnitInput(99L))))
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

        assertThatThrownBy(() -> context.responsibleService().save(context.record(), List.of(new ResponsibleUnitInput(88L))))
            .isInstanceOf(InvalidReferenceException.class)
            .hasMessage("La Unidad Orgánica pertenece a otra Unidad Ejecutora");
        verifyNoInteractions(context.responsible());
    }

    @Test void guardaUnidadActivaDeLaMismaEjecutoraConSnapshotDerivado() {
        TestContext context = context();
        OrganizationalUnitEntity active = new OrganizationalUnitEntity(context.record().getExecutingUnit(), "UO-1", "Unidad válida", "UV");
        ReflectionTestUtils.setField(active, "id", 8L);
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(active));

        context.responsibleService().save(context.record(), List.of(new ResponsibleUnitInput(8L)));

        verify(context.responsible()).save(argThat(saved -> saved.getOrganizationalUnit() == active
            && saved.getOriginalDesignation().equals("Unidad válida") && saved.getDisplayOrder() == 1));
    }

    @Test void guardaVariasUnidadesActivasConOrdenContinuoYDenominacionDelMaestro() {
        TestContext context = context();
        OrganizationalUnitEntity first = organizationalUnit(context, 8L, true, "U8");
        OrganizationalUnitEntity second = organizationalUnit(context, 9L, true, "U9");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(first));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(second));

        context.responsibleService().save(context.record(),
            List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        var saved = ArgumentCaptor.forClass(ResponsibleUnitEntity.class);
        verify(context.responsible(), times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
        assertThat(saved.getAllValues()).extracting(unit -> unit.getOrganizationalUnit().getId())
            .containsExactly(8L, 9L);
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getOriginalDesignation)
            .containsExactly("Unidad 8", "Unidad 9");
    }

    @Test void rechazaListaVaciaEnAltaSinConsultarElMaestroOrganico() {
        TestContext context = context();

        assertThatThrownBy(() -> context.responsibleService().save(context.record(), List.of()))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("INVALID_SIZE"))
            .hasMessageContaining("al menos una unidad");
        assertThatThrownBy(() -> context.responsibleService().save(context.record(), null))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("al menos una unidad");
        verifyNoInteractions(context.organizational(), context.responsible());
    }

    @Test void rechazaListaVaciaEnReemplazoSinEliminarAsociaciones() {
        TestContext context = context();

        assertThatThrownBy(() -> context.responsibleService().replace(context.record(), List.of()))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("INVALID_SIZE"))
            .hasMessageContaining("al menos una unidad");
        verifyNoInteractions(context.organizational(), context.responsible());
    }

    @Test void rechazaUnidadDuplicadaEnAltaIdentificandoLaFilaDuplicada() {
        TestContext context = context();

        assertThatThrownBy(() -> context.responsibleService().save(context.record(),
            List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(5L), new ResponsibleUnitInput(8L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception -> {
                assertThat(exception.getReason()).isEqualTo("DUPLICATED_UNIT");
                assertThat(exception.getReferenceField()).isEqualTo("responsibleUnits[3]");
                assertThat(exception.getReferenceId()).isEqualTo(8L);
            })
            .hasMessageContaining("fila 3").hasMessageContaining("fila 1");
        verifyNoInteractions(context.organizational(), context.responsible());
    }

    @Test void rechazaUnidadDuplicadaEnReemplazoSinTocarPersistencia() {
        TestContext context = context();

        assertThatThrownBy(() -> context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception -> {
                assertThat(exception.getReason()).isEqualTo("DUPLICATED_UNIT");
                assertThat(exception.getReferenceField()).isEqualTo("responsibleUnits[2]");
            })
            .hasMessageContaining("fila 2");
        verifyNoInteractions(context.organizational(), context.responsible());
    }

    @Test void rechazaNuevaIncorporacionSinSiglaEnAltaIdentificandoLaFila() {
        TestContext context = context();
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(organizationalUnit(context, 8L, true, "U8")));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(organizationalUnit(context, 9L, true, null)));

        assertThatThrownBy(() -> context.responsibleService().save(context.record(),
            List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception -> {
                assertThat(exception.getReason()).isEqualTo("MISSING_ACRONYM");
                assertThat(exception.getReferenceField()).isEqualTo("responsibleUnits[2]");
                assertThat(exception.getReferenceId()).isEqualTo(9L);
            })
            .hasMessageContaining("fila 2").hasMessageContaining("sigla");
        verifyNoInteractions(context.responsible());
    }

    @Test void rechazaNuevaIncorporacionConSiglaEnBlancoEnReemplazoSinModificarAsociaciones() {
        TestContext context = context();
        ReflectionTestUtils.setField(context.record(), "id", 50L);
        OrganizationalUnitEntity retained = organizationalUnit(context, 8L, true, "U8");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(retained));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(organizationalUnit(context, 9L, true, "   ")));
        ResponsibleUnitEntity current = new ResponsibleUnitEntity(context.record(), retained, retained.getName(), 1);
        when(context.responsible().findByRecordIdOrderByDisplayOrder(50L)).thenReturn(List.of(current));

        assertThatThrownBy(() -> context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(9L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception -> {
                assertThat(exception.getReason()).isEqualTo("MISSING_ACRONYM");
                assertThat(exception.getReferenceField()).isEqualTo("responsibleUnits[2]");
            })
            .hasMessageContaining("fila 2");
        verify(context.responsible(), never()).deleteAll(any(Iterable.class));
        verify(context.responsible(), never()).save(any());
    }

    @Test void conservaRetenidasInactivasComoContextoAlReemplazar() {
        TestContext context = context();
        ReflectionTestUtils.setField(context.record(), "id", 50L);
        OrganizationalUnitEntity retainedInactive = organizationalUnit(context, 8L, false, "U8");
        OrganizationalUnitEntity nueva = organizationalUnit(context, 9L, true, "U9");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(retainedInactive));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(nueva));
        ResponsibleUnitEntity current = new ResponsibleUnitEntity(context.record(), retainedInactive,
            retainedInactive.getName(), 1);
        when(context.responsible().findByRecordIdOrderByDisplayOrder(50L)).thenReturn(List.of(current));

        context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(9L)));

        verify(context.responsible()).deleteAll(List.of(current));
        var saved = ArgumentCaptor.forClass(ResponsibleUnitEntity.class);
        verify(context.responsible(), times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
        assertThat(saved.getAllValues()).extracting(unit -> unit.getOrganizationalUnit().getId())
            .containsExactly(8L, 9L);
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getOriginalDesignation)
            .containsExactly("Unidad 8", "Unidad 9");
    }

    @Test void conservaRetenidasSinSiglaComoContextoAlReemplazar() {
        TestContext context = context();
        ReflectionTestUtils.setField(context.record(), "id", 50L);
        OrganizationalUnitEntity retainedWithoutAcronym = organizationalUnit(context, 8L, true, null);
        OrganizationalUnitEntity nueva = organizationalUnit(context, 9L, true, "U9");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(retainedWithoutAcronym));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(nueva));
        ResponsibleUnitEntity current = new ResponsibleUnitEntity(context.record(), retainedWithoutAcronym,
            retainedWithoutAcronym.getName(), 1);
        when(context.responsible().findByRecordIdOrderByDisplayOrder(50L)).thenReturn(List.of(current));

        context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(9L)));

        verify(context.responsible()).deleteAll(List.of(current));
        verify(context.responsible(), times(2)).save(any(ResponsibleUnitEntity.class));
    }

    @Test void reemplazoConFilaInvalidaNoModificaAsociacionesPrevias() {
        TestContext context = context();
        ReflectionTestUtils.setField(context.record(), "id", 50L);
        OrganizationalUnitEntity retained = organizationalUnit(context, 8L, true, "U8");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(retained));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(organizationalUnit(context, 9L, false, "U9")));
        ResponsibleUnitEntity current = new ResponsibleUnitEntity(context.record(), retained, retained.getName(), 1);
        when(context.responsible().findByRecordIdOrderByDisplayOrder(50L)).thenReturn(List.of(current));

        assertThatThrownBy(() -> context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(9L))))
            .isInstanceOfSatisfying(InvalidReferenceException.class, exception ->
                assertThat(exception.getReason()).isEqualTo("INACTIVE"))
            .hasMessageContaining("inactiva");
        verify(context.responsible(), never()).deleteAll(any(Iterable.class));
        verify(context.responsible(), never()).save(any());
    }

    @Test void reemplazaEnElOrdenDeIncorporacionRenumerandoDeFormaContinua() {
        TestContext context = context();
        ReflectionTestUtils.setField(context.record(), "id", 50L);
        OrganizationalUnitEntity eight = organizationalUnit(context, 8L, true, "U8");
        OrganizationalUnitEntity nine = organizationalUnit(context, 9L, true, "U9");
        when(context.organizational().findHistoricalById(8L)).thenReturn(Optional.of(eight));
        when(context.organizational().findHistoricalById(9L)).thenReturn(Optional.of(nine));
        List<ResponsibleUnitEntity> current = List.of(
            new ResponsibleUnitEntity(context.record(), nine, nine.getName(), 1),
            new ResponsibleUnitEntity(context.record(), eight, eight.getName(), 2));
        when(context.responsible().findByRecordIdOrderByDisplayOrder(50L)).thenReturn(current);

        context.responsibleService().replace(context.record(),
            List.of(new PortfolioUpdateCommands.ResponsibleUnitUpdate(8L),
                new PortfolioUpdateCommands.ResponsibleUnitUpdate(9L)));

        verify(context.responsible()).deleteAll(current);
        var saved = ArgumentCaptor.forClass(ResponsibleUnitEntity.class);
        verify(context.responsible(), times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getDisplayOrder).containsExactly(1, 2);
        assertThat(saved.getAllValues()).extracting(unit -> unit.getOrganizationalUnit().getId())
            .containsExactly(8L, 9L);
        assertThat(saved.getAllValues()).extracting(ResponsibleUnitEntity::getOriginalDesignation)
            .containsExactly("Unidad 8", "Unidad 9");
    }

    @Test void lasCreacionesConResponsablesConservanFronteraTransaccional() throws NoSuchMethodException {
        Transactional transactional = InitiativeApplicationService.class
            .getMethod("create", InitiativeCreateRequest.class).getAnnotation(Transactional.class);
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
        InitiativeApplicationService service = new InitiativeApplicationService(records, responsible, executing, organizational,
            mock(pe.gob.midagri.piip.identity.persistence.UserRepository.class), mock(WorkTaskRepository.class),
            mock(NotificationRepository.class), mock(DocumentRepository.class), mock(CodeGeneratorService.class), authorization,
            mock(AuditService.class), mock(CatalogReferenceService.class), mock(DocumentTypeRepository.class));
        return new TestContext(service, new ResponsibleUnitService(responsible, organizational), record, responsible, organizational, executing, authorization);
    }

    /** Unidad Orgánica de la misma Unidad Ejecutora del registro, con identidad y vigencia configurables. */
    private static OrganizationalUnitEntity organizationalUnit(TestContext context, Long id, boolean active, String acronym) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(context.record().getExecutingUnit(),
            "UO-" + id, "Unidad " + id, acronym);
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "active", active);
        return value;
    }

    private record TestContext(InitiativeApplicationService service, ResponsibleUnitService responsibleService, PortfolioRecordEntity record,
            ResponsibleUnitRepository responsible, OrganizationalUnitRepository organizational,
            ExecutingUnitRepository executing, LocalAuthorizationService authorization) {}
}
