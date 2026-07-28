package pe.gob.midagri.piip.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI piipOpenApi() {
        return new OpenAPI()
            .info(new Info().title("PIIP API").version("v1"))
            .servers(List.of(new Server().url("/api/v1").description("Servidor PIIP")));
    }
}
