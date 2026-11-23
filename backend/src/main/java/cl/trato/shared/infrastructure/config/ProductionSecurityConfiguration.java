package cl.trato.shared.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Rechaza claves y cookies inseguras fuera de local/test (decisiones #27 y #51). */
@Configuration(proxyBeanMethods = false)
class ProductionSecurityConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProductionSecurityConfiguration.class);
  private static final String DEVELOPMENT_JWT_SECRET =
      "solo-para-desarrollo-local-cambiar-en-demo-0123456789";

  @Bean
  SecurityConfigurationValidator securityConfigurationValidator(Environment environment) {
    return new SecurityConfigurationValidator(environment);
  }

  static class SecurityConfigurationValidator {

    private final Environment environment;

    SecurityConfigurationValidator(Environment environment) {
      this.environment = environment;
      validate();
    }

    private void validate() {
      String[] profiles = environment.getActiveProfiles();
      if (profiles.length == 0) {
        profiles = environment.getDefaultProfiles();
      }
      if (Arrays.stream(profiles)
          .allMatch(profile -> profile.equals("local") || profile.equals("test"))) {
        return;
      }
      String secret = environment.getProperty("trato.identity.jwt.secret", "");
      if (DEVELOPMENT_JWT_SECRET.equals(secret)) {
        String message =
            "Configuración insegura: JWT_SECRET no puede usar el secreto de desarrollo de Docker"
                + " Compose (decisión #27); configura una clave propia de al menos 32 bytes.";
        LOGGER.error(message);
        throw new IllegalStateException(message);
      }
      if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
        String message =
            "Configuración insegura: JWT_SECRET debe tener al menos 32 bytes (decisión #27).";
        LOGGER.error(message);
        throw new IllegalStateException(message);
      }
      if (!environment.getProperty("trato.identity.refresh-cookie.secure", Boolean.class, true)) {
        String message =
            "Configuración insegura: REFRESH_COOKIE_SECURE debe ser true fuera de local/test"
                + " (decisión #51).";
        LOGGER.error(message);
        throw new IllegalStateException(message);
      }
    }
  }
}
