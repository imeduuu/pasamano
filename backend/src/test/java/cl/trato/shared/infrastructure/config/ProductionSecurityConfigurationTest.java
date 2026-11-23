package cl.trato.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Valida el endurecimiento de JWT y cookies para demo (decisiones #27 y #51). */
class ProductionSecurityConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(TestConfiguration.class)
          .withPropertyValues("spring.profiles.active=demo");

  @Test
  void fails_to_start_with_the_compose_development_secret() {
    contextRunner
        .withPropertyValues(
            "trato.identity.jwt.secret=solo-para-desarrollo-local-cambiar-en-demo-0123456789",
            "trato.identity.refresh-cookie.secure=true")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasRootCauseMessage(
                  "Configuración insegura: JWT_SECRET no puede usar el secreto de desarrollo"
                      + " de Docker Compose (decisión #27); configura una clave propia de al"
                      + " menos 32 bytes.");
        });
  }

  @Test
  void fails_to_start_when_jwt_secret_is_short_or_refresh_cookie_is_not_secure() {
    contextRunner
        .withPropertyValues(
            "trato.identity.jwt.secret=clave-demasiado-corta",
            "trato.identity.refresh-cookie.secure=false")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasStackTraceContaining("JWT_SECRET debe tener al menos 32 bytes");
        });
  }

  @Test
  void fails_to_start_when_refresh_cookie_is_not_secure() {
    contextRunner
        .withPropertyValues(
            "trato.identity.jwt.secret=clave-demo-privada-larga-de-32-bytes-minimo",
            "trato.identity.refresh-cookie.secure=false")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasStackTraceContaining("REFRESH_COOKIE_SECURE debe ser true")
              .hasStackTraceContaining("decisión #51");
        });
  }

  @Test
  void starts_with_a_private_long_secret_and_secure_refresh_cookie() {
    contextRunner
        .withPropertyValues(
            "trato.identity.jwt.secret=clave-demo-privada-larga-de-32-bytes-minimo",
            "trato.identity.refresh-cookie.secure=true")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ProductionSecurityConfiguration.class)
  static class TestConfiguration {}
}
