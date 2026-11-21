package cl.trato.identity.application.port;

import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: tokens de refresco (solo hashes). */
public interface RefreshTokenRepository {

  void save(RefreshToken token);

  Optional<RefreshToken> findByHash(String tokenHash);

  /** Revoca todos los tokens de una familia (detección de reutilización, cierre de sesión). */
  void revokeFamily(UUID familyId, Instant now);

  /** Revoca todos los tokens del usuario (cambio o recuperación de contraseña, suspensión). */
  void revokeAllForUser(UserId userId, Instant now);
}
