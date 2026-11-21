package cl.trato.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI 3 de la API (RNF-DOC-01), visible en /swagger-ui. */
@Configuration
public class OpenApiConfiguration {

  public static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  OpenAPI tratoOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Trato API")
                .version("v1")
                .description(
                    "API del marketplace Trato. Proyecto de demostración: los pagos usan el"
                        + " ambiente de pruebas de Transbank. Los errores siguen RFC 9457"
                        + " (Problem Details) con un campo `code` estable.")
                .license(new License().name("MIT")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
