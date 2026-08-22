package pe.gob.midagri.piip.organization.application;

public final class OrganizationReadModels {
    private OrganizationReadModels() {}

    public record InstitutionView(Long id, String code, String name) {}
    public record ExecutingUnitView(Long id, String code, String name, Long institutionId) {}
    public record OrganizationalUnitView(Long id, String code, String name, boolean active,
            String acronym, Long parentId, Long executingUnitId) {}
}
