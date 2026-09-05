package pe.gob.midagri.piip.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProfileConfigurationTest {
    @Test
    void configuraDevComoPerfilPorDefectoYValidaEnRuntime() throws IOException {
        String common = read("application.yml");
        String dev = read("application-dev.yml");
        String prod = read("application-prod.yml");
        assertThat(common).contains("default: dev", "ddl-auto: validate");
        assertThat(dev).contains("ddl-auto: validate");
        assertThat(prod).contains("ddl-auto: validate");
        assertThat(common).doesNotContain("oracle.net.tns_admin", "PIIP_DDL_AUTO");
        assertThat(common).doesNotContain("piip.bootstrap", "PIIP_BOOTSTRAP_");
    }

    @Test
    void elSeedSoloSeConfiguraEnElPerfilDestructivo() throws IOException {
        assertThat(read("application.yml")).doesNotContain("catalog-data.sql");
        assertThat(read("application-dev.yml")).doesNotContain("catalog-data.sql");
        assertThat(read("application-prod.yml")).doesNotContain("catalog-data.sql");
        assertThat(read("application-test-reset.yml")).contains("web-application-type: none", "ddl-auto: none");
    }

    private static String read(String resource) throws IOException {
        return new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
    }
}
