package pe.gob.midagri.piip.contract;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiGenerationTest {
    @LocalServerPort int port;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void generatesTheFrontendContractArtifact() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/v3/api-docs")).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
            .contains("/initiatives", "/projects/derived", "/portfolio-records/{recordCode}/documents",
                "/admin/users/administrable-scopes", "/initiatives/{code}/status-transitions",
                "/projects/{code}/status-transitions", "InitiativeStatusTransitionRequest",
                "ProjectStatusTransitionRequest")
            .contains("\"400\"", "\"403\"", "\"404\"", "\"409\"", "\"422\"")
            .contains("roleScopes", "RoleScopeResponse", "executingUnitId", "AdministrableScopeResponse",
                "AdministrableExecutingUnitResponse", "institutionWideAllowed")
            .doesNotContain("/admin/users/{userId}/status");
        assertThat(response.body())
            .contains("InitiativeStatusTransitionRequest", "ProjectStatusTransitionRequest")
            .contains("\"/initiatives/{code}/status-transitions\"", "\"/projects/{code}/status-transitions\"");
        Path output = Path.of("target", "piip-openapi.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, response.body(), StandardCharsets.UTF_8);
    }
}
