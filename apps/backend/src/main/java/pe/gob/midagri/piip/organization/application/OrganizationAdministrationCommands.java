package pe.gob.midagri.piip.organization.application;

/** Entradas de los casos de uso administrativos, independientes de HTTP. */
public final class OrganizationAdministrationCommands {
    private OrganizationAdministrationCommands() {}

    public record CreateExecutingUnit(Long institutionId, String name, Integer displayOrder) {}
    public record UpdateExecutingUnit(Long id, long expectedVersion, String name, Integer displayOrder) {}
    public record CreateOrganizationalUnit(Long executingUnitId, String name, String acronym, Boolean active) {}
    public record UpdateOrganizationalUnit(Long id, long expectedVersion, String name, String acronym) {}
}
