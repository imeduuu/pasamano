package cl.trato.identity.application;

import cl.trato.identity.domain.UserId;
import java.time.Instant;

/**
 * Resultado de iniciar o renovar la sesión.
 *
 * @param userId usuario autenticado
 * @param accessToken JWT de acceso (15 min)
 * @param accessTokenExpiresAt vencimiento del JWT
 * @param refreshToken token de refresco en claro, solo para la cookie HttpOnly
 * @param refreshTokenExpiresAt vencimiento del token de refresco (7 días)
 */
public record AuthSession(
    UserId userId,
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt) {

  @Override
  public String toString() {
    return "AuthSession[userId=" + userId + "]";
  }
}
