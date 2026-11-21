package cl.trato.identity.application.port;

import cl.trato.identity.domain.PasswordHash;

/** Puerto de salida: hash de contraseñas (BCrypt factor 12, RNF-SEG-01). */
public interface PasswordHasher {

  PasswordHash hash(String rawPassword);

  boolean matches(String rawPassword, PasswordHash hash);

  /**
   * Hace una verificación contra un hash ficticio para gastar el mismo tiempo que una real, de modo
   * que la respuesta no revele si el correo existe.
   */
  void matchesDummy(String rawPassword);
}
