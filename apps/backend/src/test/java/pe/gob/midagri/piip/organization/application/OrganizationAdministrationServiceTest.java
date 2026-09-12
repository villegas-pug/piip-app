package pe.gob.midagri.piip.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.application.LocalAuthorizationService;
import pe.gob.midagri.piip.identity.application.RoleScopeGrant;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.organization.application.OrganizationAdministrationCommands.*;
import pe.gob.midagri.piip.organization.persistence.*;
import pe.gob.midagri.piip.shared.application.error.BusinessRuleException;
import pe.gob.midagri.piip.shared.application.error.ProblemCode;
import pe.gob.midagri.piip.shared.application.error.StaleVersionException;

@ExtendWith(MockitoExtension.class)
class OrganizationAdministrationServiceTest {
    @Mock InstitutionRepository institutions;
    @Mock ExecutingUnitRepository executingUnits;
    @Mock OrganizationalUnitRepository organizationalUnits;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;

    private OrganizationAdministrationService service;
    private InstitutionEntity institution;
    private ExecutingUnitEntity executingUnit;
    private LocalAccessContext administrator;

    @BeforeEach
    void setUp() {
        institution = institution(10L);
        executingUnit = executingUnit(100L, "UE-001");
        administrator = new LocalAccessContext(1L, "admin-subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, null)));
        when(authorization.requireOrganizationAdministration(anyLong())).thenReturn(administrator);
        service = new OrganizationAdministrationService(institutions, executingUnits, organizationalUnits, authorization, audit);
    }

    @Test
    void createsAnExecutingUnitWithGeneratedCodeDatesAndAutomaticOrder() {
        when(institutions.findByIdForUpdate(10L)).thenReturn(Optional.of(institution));
        when(executingUnits.findByInstitutionIdOrderByDisplayOrderAscNameAscIdAsc(10L)).thenReturn(List.of());
        when(executingUnits.findMaxDisplayOrderByInstitutionId(10L)).thenReturn(-1);
        when(executingUnits.findByInstitutionIdAndCodeIgnoreCaseForUpdate(10L, "UE-001")).thenReturn(Optional.empty());
        when(executingUnits.save(any(ExecutingUnitEntity.class))).thenAnswer(invocation -> {
            ExecutingUnitEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 101L);
            return value;
        });

        var result = service.createExecutingUnit(new CreateExecutingUnit(10L, "Nueva UE", null));

        assertThat(result.code()).isEqualTo("UE-001");
        assertThat(result.displayOrder()).isZero();
        assertThat(result.registeredAt()).isEqualTo(result.activatedAt());
        verify(audit).event(eq("UE_CREADA"), eq("UNIDAD_EJECUTORA"), eq(101L), eq("UE-001"), eq(10L), eq(101L), eq((Long) null), any(), eq("admin-subject"));
    }

    @Test
    void rejectsStaleExecutingUnitVersionBeforeChangingOrAuditing() {
        ReflectionTestUtils.setField(executingUnit, "version", 2L);
        when(executingUnits.findByIdForUpdate(100L)).thenReturn(Optional.of(executingUnit));

        assertThatThrownBy(() -> service.updateExecutingUnit(new UpdateExecutingUnit(100L, 1L, "Cambio", 2)))
            .isInstanceOf(StaleVersionException.class);
        assertThat(executingUnit.getName()).isEqualTo("Unidad Ejecutora");
        verify(audit, never()).event(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsAnOrganizationalUnitWithIndependentGeneratedCodeAndExplicitState() {
        when(executingUnits.findByIdForUpdate(100L)).thenReturn(Optional.of(executingUnit));
        when(organizationalUnits.findByExecutingUnitIdOrderByNameAscIdAsc(100L)).thenReturn(List.of());
        when(organizationalUnits.findByExecutingUnitIdAndCodeIgnoreCaseForUpdate(100L, "UO-001")).thenReturn(Optional.empty());
        when(organizationalUnits.save(any(OrganizationalUnitEntity.class))).thenAnswer(invocation -> {
            OrganizationalUnitEntity value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 201L);
            return value;
        });

        var result = service.createOrganizationalUnit(new CreateOrganizationalUnit(100L, "Unidad Orgánica", "UO", false));

        assertThat(result.code()).isEqualTo("UO-001");
        assertThat(result.active()).isFalse();
        assertThat(result.executingUnit().id()).isEqualTo(100L);
        verify(audit).event(eq("UO_CREADA"), eq("UNIDAD_ORGANICA"), eq(201L), eq("UO-001"), eq(10L), eq(100L), eq(201L), any(), eq("admin-subject"));
    }

    @Test
    void rejectsReactivationWhenTheOrganizationalUnitHasNoAcronym() {
        OrganizationalUnitEntity unit = new OrganizationalUnitEntity(executingUnit, "UO-001", "Unidad", null, false);
        ReflectionTestUtils.setField(unit, "id", 201L);
        when(organizationalUnits.findByIdForUpdate(201L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.reactivateOrganizationalUnit(201L, 0L))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(error -> assertThat(((BusinessRuleException) error).getProblemCode())
                .isEqualTo(ProblemCode.INVALID_REQUEST));
        verify(audit, never()).event(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsGeneratedOrganizationalCodeCollisionWithoutSavingOrAuditing() {
        when(executingUnits.findByIdForUpdate(100L)).thenReturn(Optional.of(executingUnit));
        when(organizationalUnits.findByExecutingUnitIdOrderByNameAscIdAsc(100L)).thenReturn(List.of());
        when(organizationalUnits.findByExecutingUnitIdAndCodeIgnoreCaseForUpdate(100L, "UO-001"))
            .thenReturn(Optional.of(new OrganizationalUnitEntity(executingUnit, "uo-001", "Existente", "EX", true)));

        assertThatThrownBy(() -> service.createOrganizationalUnit(
            new CreateOrganizationalUnit(100L, "Nueva", "N", true)))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(error -> assertThat(((BusinessRuleException) error).getProblemCode())
                .isEqualTo(ProblemCode.ORGANIZATION_CODE_DUPLICATE));
        verify(organizationalUnits, never()).save(any());
        verify(audit, never()).event(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private InstitutionEntity institution(Long id) {
        InstitutionEntity value = new InstitutionEntity("INST-" + id, "Institución " + id);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private ExecutingUnitEntity executingUnit(Long id, String code) {
        ExecutingUnitEntity value = new ExecutingUnitEntity(institution, code, "Unidad Ejecutora", 1,
            Instant.parse("2026-09-11T12:00:00Z"));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
