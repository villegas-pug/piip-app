package pe.gob.midagri.piip.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI piipOpenApi() {
        return new OpenAPI()
            .info(new Info().title("PIIP API").version("v1"))
            .components(new Components().addSchemas("ProblemDetail", problemDetailSchema()))
            .servers(List.of(new Server().url("/api/v1").description("Servidor PIIP")));
    }

    @Bean
    OpenApiCustomizer piipProblemDetailSchemaCustomizer() {
        return openApi -> openApi.getComponents().addSchemas("ProblemDetail", problemDetailSchema());
    }

    private Schema<Object> problemDetailSchema() {
        Schema<Object> problemDetail = new Schema<>()
            .type("object")
            .addProperty("type", new StringSchema().format("uri"))
            .addProperty("title", new StringSchema())
            .addProperty("status", new Schema<>().type("integer"))
            .addProperty("detail", new StringSchema())
            .addProperty("instance", new StringSchema().format("uri"))
            .addProperty("problemCode", new StringSchema()._enum(List.of(
                "INVALID_REQUEST", "FORBIDDEN_SCOPE", "RESOURCE_NOT_FOUND", "STALE_VERSION",
                "ACTIVE_ASSIGNMENT_DUPLICATE", "SELF_ADMIN_SUSPENSION", "LAST_ACTIVE_ADMIN",
                "INCOMPATIBLE_ASSIGNMENT_STATE", "INVALID_ACTIVE_REFERENCE", "BUSINESS_RULE_VIOLATION")));
        problemDetail.required(List.of("type", "title", "status", "detail", "problemCode"));
        return problemDetail;
    }
}
