package pe.gob.midagri.piip.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.audit.application.AuditService;
import pe.gob.midagri.piip.catalogs.application.CatalogReferenceService;
import pe.gob.midagri.piip.catalogs.domain.CatalogCode;
import pe.gob.midagri.piip.documents.application.PortfolioDocumentService;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitEntity;
import pe.gob.midagri.piip.organization.persistence.OrganizationalUnitRepository;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.DerivedProjectRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.InitiativeCreateRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PreexistingProjectRequest;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.ResponsibleUnitInput;
import pe.gob.midagri.piip.portfolio.domain.DigitalComponent;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitEntity;
import pe.gob.midagri.piip.portfolio.persistence.ResponsibleUnitRepository;
import pe.gob.midagri.piip.shared.application.error.InvalidReferenceException;
import pe.gob.midagri.piip.support.PortfolioRecordTestBuilder;
import pe.gob.midagri.piip.work.application.PortfolioWorkService;
import pe.gob.midagri.piip.work.persistence.NotificationRepository;
import pe.gob.midagri.piip.work.persistence.WorkTaskRepository;

/**
 * Auditoría de las altas (FR-018/FR-020/FR-021): los tres tipos de registro auditan la lista
 * ordenada confirmada con unidad, nombre, sigla y Nro; una alta rechazada no emite evento y
 * el detalle nunca transporta cuerpos de solicitud ni datos sensibles.
 */
class PortfolioRegistrationAuditTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 7);

    @Test
    void iniciativaRegistradaAuditaLaListaOrdenadaConfirmada() {
        RegistrationContext context = context();
        stubUnidadesValidas(context);
        InitiativeCreateRequest request = new InitiativeCreateRequest(5L, "Iniciativa auditada", 11L, 12L,
            START_DATE, "Responsable", null, null, "Descripción", null, DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        context.initiatives().createInitiative(request);

        Map<String, ?> detail = capturarDetalle(context, "INICIATIVA_REGISTRADA", "I-AUD-01");
        assertThat(detail).containsOnlyKeys("estado", "responsibleUnits");
        assertThat(detail.get("estado")).isEqualTo("Presentado");
        asertarListaOrdenadaConfirmada(detail.get("responsibleUnits"));
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
    }

    @Test
    void proyectoDerivadoRegistradoAuditaLaListaOrdenadaConfirmada() {
        RegistrationContext context = context();
        stubUnidadesValidas(context);
        PortfolioRecordEntity origin = PortfolioRecordTestBuilder.transientReferences()
            .initiative("I-ORIG-AUD", context.executingUnit(), "Iniciativa origen");
        ReflectionTestUtils.setField(origin, "id", 102L);
        origin.approve();
        when(context.records().findByCodeIgnoreCaseForUpdate("I-ORIG-AUD")).thenReturn(Optional.of(origin));
        when(context.records().existsByOriginRecordId(102L)).thenReturn(false);
        DerivedProjectRequest request = new DerivedProjectRequest("I-ORIG-AUD", START_DATE,
            "Proyecto derivado auditado", 11L, 12L, "Responsable", null, null, "Descripción", null, null,
            DigitalComponent.NO, List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        context.projects().createDerived(request);

        Map<String, ?> detail = capturarDetalle(context, "PROYECTO_DERIVADO_REGISTRADO", "P-AUD-01");
        assertThat(detail).containsOnlyKeys("iniciativaOrigen", "responsibleUnits");
        assertThat(detail.get("iniciativaOrigen")).isEqualTo("I-ORIG-AUD");
        asertarListaOrdenadaConfirmada(detail.get("responsibleUnits"));
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
    }

    @Test
    void proyectoPreexistenteRegistradoAuditaLaListaOrdenadaConfirmada() {
        RegistrationContext context = context();
        stubUnidadesValidas(context);
        when(context.catalogReferences().resolveActiveByCode(CatalogCode.SOLUTION_TYPE, "NOT_APPLICABLE", "solutionTypeId"))
            .thenReturn(PortfolioRecordTestBuilder.transientReferences().solution());
        PreexistingProjectRequest request = new PreexistingProjectRequest(5L, START_DATE,
            "Proyecto preexistente auditado", 12L, "Responsable", null, null, "Descripción", null, null,
            DigitalComponent.NO, List.of(new ResponsibleUnitInput(8L), new ResponsibleUnitInput(9L)));

        context.projects().createPreexisting(request);

        Map<String, ?> detail = capturarDetalle(context, "PROYECTO_PREEXISTENTE_REGISTRADO", "P-AUD-01");
        assertThat(detail).containsOnlyKeys("origen", "responsibleUnits");
        assertThat(detail.get("origen")).isEqualTo("NA");
        asertarListaOrdenadaConfirmada(detail.get("responsibleUnits"));
        assertThat(detail).doesNotContainKeys("request", "body", "token", "motivo");
    }

    @Test
    void altaRechazadaPorUnidadInactivaNoEmiteEventoDeRegistro() {
        RegistrationContext context = context();
        when(context.organizationalUnits().findHistoricalById(8L)).thenReturn(Optional.of(unidad(context, 8L, false, "UA1")));
        InitiativeCreateRequest request = new InitiativeCreateRequest(5L, "Iniciativa rechazada", 11L, 12L,
            START_DATE, "Responsable", null, null, "Descripción", null, DigitalComponent.NO,
            List.of(new ResponsibleUnitInput(8L)));

        assertThatThrownBy(() -> context.initiatives().createInitiative(request))
            .isInstanceOf(InvalidReferenceException.class).hasMessageContaining("inactiva");
        verify(context.audit(), never()).event(any(), any(), any(), any(), any());
    }

    /** FR-020: cada elemento identifica unidad (identidad/código), nombre, sigla y Nro en el orden confirmado. */
    @SuppressWarnings("unchecked")
    private static void asertarListaOrdenadaConfirmada(Object audited) {
        List<?> units = (List<?>) audited;
        assertThat(units).hasSize(2);
        // K=String en los maps auditados para que AssertJ acepte las claves varargs de contains*.
        assertThat((Map<String, Object>) units.get(0)).containsOnlyKeys("id", "code", "name", "sigla", "nro")
            .containsEntry("id", 8L).containsEntry("code", "UE-AUD-UO-01").containsEntry("name", "Unidad auditoría 1")
            .containsEntry("sigla", "UA1").containsEntry("nro", 1);
        assertThat((Map<String, Object>) units.get(1)).containsEntry("id", 9L).containsEntry("code", "UE-AUD-UO-02")
            .containsEntry("name", "Unidad auditoría 2").containsEntry("sigla", "UA2").containsEntry("nro", 2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, ?> capturarDetalle(RegistrationContext context, String eventType, String entityCode) {
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass((Class) Map.class);
        verify(context.audit()).event(eq(eventType), eq("REGISTRO_PORTAFOLIO"), eq(entityCode),
            captor.capture(), eq("actor-audit"));
        return captor.getValue();
    }

    private static void stubUnidadesValidas(RegistrationContext context) {
        when(context.organizationalUnits().findHistoricalById(8L)).thenReturn(Optional.of(unidad(context, 8L, true, "UA1")));
        when(context.organizationalUnits().findHistoricalById(9L)).thenReturn(Optional.of(unidad(context, 9L, true, "UA2")));
    }

    private static OrganizationalUnitEntity unidad(RegistrationContext context, Long id, boolean active, String sigla) {
        OrganizationalUnitEntity value = new OrganizationalUnitEntity(context.executingUnit(),
            "UE-AUD-UO-0" + (id - 7), "Unidad auditoría " + (id - 7), sigla);
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "active", active);
        return value;
    }

    private static RegistrationContext context() {
        PortfolioRecordRepository records = mock(PortfolioRecordRepository.class);
        ResponsibleUnitRepository responsibleUnits = mock(ResponsibleUnitRepository.class);
        ExecutingUnitRepository executingUnits = mock(ExecutingUnitRepository.class);
        OrganizationalUnitRepository organizationalUnits = mock(OrganizationalUnitRepository.class);
        UserRepository users = mock(UserRepository.class);
        WorkTaskRepository tasks = mock(WorkTaskRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        CodeGeneratorService codes = mock(CodeGeneratorService.class);
        LocalAuthorizationService authorization = mock(LocalAuthorizationService.class);
        AuditService audit = mock(AuditService.class);
        CatalogReferenceService catalogReferences = mock(CatalogReferenceService.class);
        DocumentTypeRepository documentTypes = mock(DocumentTypeRepository.class);

        InstitutionEntity institution = new InstitutionEntity("INST-AUD-ALTA", "Institución");
        ReflectionTestUtils.setField(institution, "id", 30L);
        ExecutingUnitEntity executingUnit = new ExecutingUnitEntity(institution, "UE-AUD-ALTA", "Unidad Ejecutora auditoría");
        ReflectionTestUtils.setField(executingUnit, "id", 5L);

        var fixtures = PortfolioRecordTestBuilder.transientReferences();
        when(authorization.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 5L))
            .thenReturn(new LocalAccessContext(1L, "actor-audit", Set.of()));
        when(executingUnits.findById(5L)).thenReturn(Optional.of(executingUnit));
        when(codes.next(RecordType.INITIATIVE, 2026)).thenReturn("I-AUD-01");
        when(codes.next(RecordType.PROJECT, 2026)).thenReturn("P-AUD-01");
        when(catalogReferences.resolveActive(11L, CatalogCode.SOLUTION_TYPE, "solutionTypeId")).thenReturn(fixtures.solution());
        when(catalogReferences.resolveActive(12L, CatalogCode.SOURCE_ORIGIN, "sourceId")).thenReturn(fixtures.source());
        when(users.findById(1L)).thenReturn(Optional.of(new UserEntity("actor-audit", "Actor Auditoría", "actor@midagri.gob.pe")));
        when(tasks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // El repositorio devuelve las asociaciones persistidas en su orden 1..N para el evento de auditoría.
        List<ResponsibleUnitEntity> persisted = new ArrayList<>();
        when(responsibleUnits.save(any(ResponsibleUnitEntity.class))).thenAnswer(invocation -> {
            ResponsibleUnitEntity saved = invocation.getArgument(0);
            persisted.add(saved);
            return saved;
        });
        when(responsibleUnits.findByRecordIdOrderByDisplayOrder(any())).thenAnswer(invocation -> List.copyOf(persisted));
        PortfolioRecordEntity[] savedRecord = new PortfolioRecordEntity[1];
        when(records.save(any(PortfolioRecordEntity.class))).thenAnswer(invocation -> {
            PortfolioRecordEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 100L);
            savedRecord[0] = value;
            return value;
        });
        when(records.findById(100L)).thenAnswer(invocation -> Optional.of(savedRecord[0]));

        InitiativeApplicationService initiatives = new InitiativeApplicationService(records, responsibleUnits,
            executingUnits, organizationalUnits, users, tasks, notifications, documents, codes, authorization, audit,
            catalogReferences, documentTypes);
        // El constructor principal permite inyectar el ResponsibleUnitService con el maestro organizacional:
        // los constructores de compatibilidad de Project lo dejan en null y el alta de proyectos resuelve unidades.
        ProjectApplicationService projects = new ProjectApplicationService(records, executingUnits, tasks, codes,
            authorization, audit, catalogReferences, new ResponsibleUnitService(responsibleUnits, organizationalUnits),
            new PortfolioDocumentService(records, documents, documentTypes),
            new PortfolioWorkService(tasks, notifications, audit),
            new PortfolioApplicationSupport(authorization, Clock.systemUTC()),
            new PortfolioReadModelAssembler(responsibleUnits));
        return new RegistrationContext(initiatives, projects, records, responsibleUnits, organizationalUnits,
            executingUnit, audit, catalogReferences);
    }

    private record RegistrationContext(InitiativeApplicationService initiatives, ProjectApplicationService projects,
            PortfolioRecordRepository records, ResponsibleUnitRepository responsibleUnits,
            OrganizationalUnitRepository organizationalUnits, ExecutingUnitEntity executingUnit,
            AuditService audit, CatalogReferenceService catalogReferences) {}
}
