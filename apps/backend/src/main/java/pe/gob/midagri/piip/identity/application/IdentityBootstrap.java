package pe.gob.midagri.piip.identity.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.config.PiipProperties;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.identity.persistence.*;
import pe.gob.midagri.piip.organization.persistence.*;
import java.util.Arrays;

@Component
public class IdentityBootstrap implements ApplicationRunner {
    private final PiipProperties.Bootstrap properties;
    private final RoleRepository roles;
    private final UserRepository users;
    private final UserRoleScopeRepository scopes;
    private final InstitutionRepository institutions;
    private final ExecutingUnitRepository executingUnits;
    private final Environment environment;

    public IdentityBootstrap(PiipProperties.Bootstrap properties, RoleRepository roles, UserRepository users,
            UserRoleScopeRepository scopes, InstitutionRepository institutions, ExecutingUnitRepository executingUnits,
            Environment environment) {
        this.properties = properties; this.roles = roles; this.users = users; this.scopes = scopes;
        this.institutions = institutions; this.executingUnits = executingUnits;
        this.environment = environment;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        RoleEntity administrator = roles.findByCode(RoleCode.ADMINISTRADOR_PIIP)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.ADMINISTRADOR_PIIP, "Administrador PIIP")));
        roles.findByCode(RoleCode.CONSULTA_EXTERNA)
            .orElseGet(() -> roles.save(new RoleEntity(RoleCode.CONSULTA_EXTERNA, "Consulta externa")));

        if (scopes.countActiveByRole(RoleCode.ADMINISTRADOR_PIIP, java.time.Instant.now()) > 0) return;
        if (!properties.enabled()) {
            if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                throw new IllegalStateException("Producción requiere un administrador local activo o un bootstrap explícito");
            }
            return;
        }
        require(properties.subject(), "PIIP_BOOTSTRAP_SUBJECT");
        require(properties.institutionCode(), "PIIP_BOOTSTRAP_INSTITUTION_CODE");

        InstitutionEntity institution = institutions.findByCodeIgnoreCase(properties.institutionCode())
            .orElseGet(() -> institutions.save(new InstitutionEntity(properties.institutionCode(), properties.institutionCode())));
        UserEntity admin = users.findByKeycloakSubject(properties.subject())
            .orElseGet(() -> users.save(new UserEntity(properties.subject(), valueOr(properties.name(), properties.subject()), valueOr(properties.email(), properties.subject() + "@unknown.local"))));

        if (properties.executingUnitCodes() == null || properties.executingUnitCodes().isEmpty()) {
            scopes.save(new UserRoleScopeEntity(admin, administrator, institution, null, "BOOTSTRAP"));
            return;
        }
        for (String code : properties.executingUnitCodes()) {
            ExecutingUnitEntity unit = executingUnits.findByInstitutionIdAndCodeIgnoreCase(institution.getId(), code)
                .orElseGet(() -> executingUnits.save(new ExecutingUnitEntity(institution, code, code)));
            scopes.save(new UserRoleScopeEntity(admin, administrator, institution, unit, "BOOTSTRAP"));
        }
    }

    private void require(String value, String variable) { if (value == null || value.isBlank()) throw new IllegalStateException(variable + " es obligatorio para el bootstrap"); }
    private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
