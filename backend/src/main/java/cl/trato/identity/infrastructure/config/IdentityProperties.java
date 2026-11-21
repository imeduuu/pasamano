package cl.trato.identity.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de identidad, leída de variables de entorno (12-factor).
 *
 * @param jwt firma y vigencia del token de acceso
 * @param refreshCookie atributos de la cookie del token de refresco
 * @param legal versiones vigentes de términos y privacidad
 * @param loginRateLimit límite de intentos de inicio de sesión por IP y correo
 */
@ConfigurationProperties(prefix = "trato.identity")
public record IdentityProperties(
    Jwt jwt, RefreshCookie refreshCookie, Legal legal, LoginRateLimit loginRateLimit) {

  /**
   * Token de acceso.
   *
   * @param secret clave HMAC de al menos 32 bytes
   * @param issuer emisor ({@code iss})
   * @param accessTokenTtl vigencia (15 min, RF-AUT-03)
   */
  public record Jwt(String secret, String issuer, Duration accessTokenTtl) {

    public Jwt {
      if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
        throw new IllegalStateException(
            "JWT_SECRET debe tener al menos 32 bytes (HS256). Revisa las variables de entorno.");
      }
      issuer = issuer == null ? "trato" : issuer;
      accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
    }

    @Override
    public String toString() {
      return "Jwt[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + "]";
    }
  }

  /**
   * Cookie del token de refresco (HttpOnly, SameSite=Strict).
   *
   * @param name nombre de la cookie
   * @param path ruta a la que se envía
   * @param secure atributo Secure
   */
  public record RefreshCookie(String name, String path, boolean secure) {}

  /**
   * Versiones de los documentos legales.
   *
   * @param termsVersion términos y condiciones
   * @param privacyVersion política de privacidad
   */
  public record Legal(String termsVersion, String privacyVersion) {}

  /**
   * Límite de inicio de sesión (RNF-SEG-05: 5/min por IP y correo).
   *
   * @param capacity intentos por periodo
   * @param period periodo
   */
  public record LoginRateLimit(int capacity, Duration period) {}
}
