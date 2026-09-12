package pe.gob.midagri.piip.organization.application;

import java.time.Instant;

/** Representaciones administrativas que no filtran entidades JPA hacia el adapter HTTP. */
public final class OrganizationAdministrationReadModels {
    private OrganizationAdministrationReadModels() {}

    public record Institution(Long id, String code, String name) {}

    public record InstitutionReference(Long id, String code, String name) {}

    public record ExecutingUnit(Long id, String code, String name, boolean active, int displayOrder,
            Instant registeredAt, Instant activatedAt, long version, InstitutionReference institution) {}

    public record ExecutingUnitReference(Long id, String code, String name) {}

    public record OrganizationalUnit(Long id, String code, String name, String acronym, boolean active,
            long version, ExecutingUnitReference executingUnit) {}
}
