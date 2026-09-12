package pe.gob.midagri.piip.organization.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
import pe.gob.midagri.piip.organization.persistence.*;

@ExtendWith(MockitoExtension.class)
class OrganizationAdministrationTransactionalAuditTest {
    @Mock InstitutionRepository institutions;
    @Mock ExecutingUnitRepository executingUnits;
    @Mock OrganizationalUnitRepository organizationalUnits;
    @Mock LocalAuthorizationService authorization;
    @Mock AuditService audit;

    @Test
    void rejectedVersionDoesNotEmitAFunctionalSuccessEvent() {
        InstitutionEntity institution = institution(10L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-001", "UE", 0,
            Instant.parse("2026-09-11T12:00:00Z"));
        ReflectionTestUtils.setField(unit, "id", 100L);
        ReflectionTestUtils.setField(unit, "version", 3L);
        when(executingUnits.findByIdForUpdate(100L)).thenReturn(Optional.of(unit));
        when(authorization.requireOrganizationAdministration(anyLong())).thenReturn(administrator());

        OrganizationAdministrationService service = new OrganizationAdministrationService(institutions, executingUnits,
            organizationalUnits, authorization, audit);

        assertThatThrownBy(() -> service.updateExecutingUnit(
            new OrganizationAdministrationCommands.UpdateExecutingUnit(100L, 2L, "Cambio", 1)))
            .isInstanceOf(pe.gob.midagri.piip.shared.application.error.StaleVersionException.class);
        verify(audit, never()).event(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void propagatesFunctionalAuditFailureFromTheMutationBoundary() {
        InstitutionEntity institution = institution(10L);
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-001", "UE", 0,
            Instant.parse("2026-09-11T12:00:00Z"));
        ReflectionTestUtils.setField(unit, "id", 100L);
        when(executingUnits.findByIdForUpdate(100L)).thenReturn(Optional.of(unit));
        when(authorization.requireOrganizationAdministration(anyLong())).thenReturn(administrator());
        doThrow(new IllegalStateException("audit unavailable")).when(audit).event(eq("UE_DESACTIVADA"), eq("UNIDAD_EJECUTORA"),
            eq(100L), eq("UE-001"), eq(10L), eq(100L), eq((Long) null), any(), eq("admin-subject"));

        OrganizationAdministrationService service = new OrganizationAdministrationService(institutions, executingUnits,
            organizationalUnits, authorization, audit);

        assertThatThrownBy(() -> service.deactivateExecutingUnit(100L, 0L))
            .isInstanceOf(IllegalStateException.class).hasMessage("audit unavailable");
    }

    private LocalAccessContext administrator() {
        return new LocalAccessContext(1L, "admin-subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 10L, null)));
    }

    private InstitutionEntity institution(Long id) {
        InstitutionEntity value = new InstitutionEntity("INST-" + id, "Institución");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
