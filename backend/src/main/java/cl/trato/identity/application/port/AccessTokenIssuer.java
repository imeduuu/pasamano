package cl.trato.identity.application.port;

import cl.trato.identity.domain.User;
import java.time.Instant;

/** Puerto de salida: emisión del token de acceso JWT de 15 minutos (RF-AUT-03). */
public interface AccessTokenIssuer {

  IssuedAccessToken issue(User user, Instant now);

  /**
   * Token de acceso emitido.
   *
   * @param token JWT firmado
   * @param expiresAt vencimiento
   */
  record IssuedAccessToken(String token, Instant expiresAt) {}
}
