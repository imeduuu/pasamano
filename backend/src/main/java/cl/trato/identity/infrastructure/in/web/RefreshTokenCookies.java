package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.infrastructure.config.IdentityProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie del token de refresco: {@code HttpOnly}, {@code Secure} y {@code SameSite=Strict}, con
 * ruta limitada a {@code /api/v1/auth} (RNF-SEG-02, ADR-002).
 */
@Component
class RefreshTokenCookies {

  private final IdentityProperties.RefreshCookie properties;

  RefreshTokenCookies(IdentityProperties properties) {
    this.properties = properties.refreshCookie();
  }

  String name() {
    return properties.name();
  }

  ResponseCookie issue(String rawToken, Instant expiresAt, Instant now) {
    return base(rawToken).maxAge(Duration.between(now, expiresAt)).build();
  }

  ResponseCookie clear() {
    return base("").maxAge(Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(properties.name(), value)
        .httpOnly(true)
        .secure(properties.secure())
        .sameSite("Strict")
        .path(properties.path());
  }
}
