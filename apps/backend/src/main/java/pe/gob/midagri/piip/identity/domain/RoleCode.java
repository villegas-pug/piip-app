package pe.gob.midagri.piip.identity.domain;

public enum RoleCode {
    ADMINISTRADOR_PIIP,
    CONSULTA_EXTERNA;

    public String authority() { return "ROLE_" + name(); }
}
