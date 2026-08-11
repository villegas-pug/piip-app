package pe.gob.midagri.piip.identity.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "USUARIO", uniqueConstraints = @UniqueConstraint(name = "UK_USUARIO_SUBJECT", columnNames = "KEYCLOAK_SUBJECT"))
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO") private Long id;
    @Column(name = "KEYCLOAK_SUBJECT", length = 100, nullable = false) private String keycloakSubject;
    @Column(name = "NOMBRE_COMPLETO", length = 300, nullable = false) private String fullName;
    @Column(name = "CORREO", length = 200, nullable = false) private String email;
    @Column(name = "ACTIVO", nullable = false) private boolean active = true;
    @Column(name = "ULTIMA_AUTENTICACION") private Instant lastAuthenticatedAt;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected UserEntity() {}
    public UserEntity(String keycloakSubject, String fullName, String email) {
        this.keycloakSubject = keycloakSubject; this.fullName = fullName; this.email = email;
    }
    public Long getId() { return id; }
    public String getKeycloakSubject() { return keycloakSubject; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public Instant getLastAuthenticatedAt() { return lastAuthenticatedAt; }
    public long getVersion() { return version; }
    public void recordAuthentication(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName;
        if (email != null && !email.isBlank()) this.email = email;
        this.lastAuthenticatedAt = Instant.now();
    }
    public void changeActiveState(boolean active) { this.active = active; }
}
