package pe.gob.midagri.piip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

public final class PiipProperties {
    private PiipProperties() {}

    @ConfigurationProperties("piip.security")
    public record Security(String audience, List<String> allowedOrigins) {}

    @ConfigurationProperties("piip.documents")
    public record Documents(long maxSizeBytes) {}

    @ConfigurationProperties("piip.audit")
    public record Audit(int retentionDays) {}

    @ConfigurationProperties("piip.bootstrap")
    public record Bootstrap(boolean enabled, String subject, String name, String email,
            String institutionCode, List<String> executingUnitCodes) {}
}
