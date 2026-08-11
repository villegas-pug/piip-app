package pe.gob.midagri.piip.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.RoleEntity;
import pe.gob.midagri.piip.identity.persistence.UserEntity;
import pe.gob.midagri.piip.identity.persistence.UserRepository;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeEntity;
import pe.gob.midagri.piip.identity.persistence.UserRoleScopeRepository;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitEntity;
import pe.gob.midagri.piip.organization.persistence.ExecutingUnitRepository;
import pe.gob.midagri.piip.organization.persistence.InstitutionEntity;

@ExtendWith(MockitoExtension.class)
class LocalAuthorizationServiceTest {
    @Mock UserRepository users;
    @Mock UserRoleScopeRepository scopes;
    @Mock ExecutingUnitRepository executingUnits;
    @Mock Authentication authentication;
    private LocalAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new LocalAuthorizationService(users, scopes, executingUnits);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorInAnotherUnitCannotWriteAReadableConsultationUnit() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
                new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authentication.getDetails()).thenReturn(context);
        when(executingUnits.findById(100L)).thenReturn(Optional.of(unit(100L, 10L)));

        assertThat(service.requireReadableUnit(100L)).isSameAs(context);
        assertThatThrownBy(() -> service.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 100L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("fuera del ámbito autorizado");
    }

    @Test
    void administratorGrantCoversItsOwnUnit() {
        LocalAccessContext context = new LocalAccessContext(1L, "subject",
            Set.of(new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, 200L)));
        when(authentication.getDetails()).thenReturn(context);
        when(executingUnits.findById(200L)).thenReturn(Optional.of(unit(200L, 20L)));

        assertThat(service.requireUnit(RoleCode.ADMINISTRADOR_PIIP, 200L)).isSameAs(context);
    }

    @Test
    void resolvePreservesOnlyTheExactGrantsReturnedAsActiveAndCurrent() {
        UserEntity user = new UserEntity("subject", "Persona", "persona@example.test");
        ReflectionTestUtils.setField(user, "id", 1L);
        InstitutionEntity institutionOne = institution(10L);
        InstitutionEntity institutionTwo = institution(20L);
        ExecutingUnitEntity unitOne = unit(100L, institutionOne);
        RoleEntity consultation = new RoleEntity(RoleCode.CONSULTA_EXTERNA, "Consulta externa");
        RoleEntity administrator = new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP");
        UserRoleScopeEntity consultationScope = new UserRoleScopeEntity(user, consultation, institutionOne, unitOne, "TEST");
        UserRoleScopeEntity institutionalAdministrator = new UserRoleScopeEntity(user, administrator, institutionTwo, null, "TEST");
        when(users.findByKeycloakSubject("subject")).thenReturn(Optional.of(user));
        when(scopes.findActiveBySubject(org.mockito.ArgumentMatchers.eq("subject"), org.mockito.ArgumentMatchers.any(Instant.class)))
            .thenReturn(List.of(consultationScope, institutionalAdministrator));

        LocalAccessContext resolved = service.resolve("subject");

        assertThat(resolved.grants()).containsExactlyInAnyOrder(
            new RoleScopeGrant(RoleCode.CONSULTA_EXTERNA, 10L, 100L),
            new RoleScopeGrant(RoleCode.ADMINISTRADOR_PIIP, 20L, null));
        verify(scopes).findActiveBySubject(org.mockito.ArgumentMatchers.eq("subject"), org.mockito.ArgumentMatchers.any(Instant.class));
    }

    private ExecutingUnitEntity unit(Long unitId, Long institutionId) {
        return unit(unitId, institution(institutionId));
    }

    private InstitutionEntity institution(Long institutionId) {
        InstitutionEntity institution = new InstitutionEntity("INST-" + institutionId, "Institución");
        ReflectionTestUtils.setField(institution, "id", institutionId);
        return institution;
    }

    private ExecutingUnitEntity unit(Long unitId, InstitutionEntity institution) {
        ExecutingUnitEntity unit = new ExecutingUnitEntity(institution, "UE-" + unitId, "Unidad Ejecutora");
        ReflectionTestUtils.setField(unit, "id", unitId);
        return unit;
    }
}
