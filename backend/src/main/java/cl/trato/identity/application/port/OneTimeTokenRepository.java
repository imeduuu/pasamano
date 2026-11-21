package cl.trato.identity.application.port;

import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;

/** Puerto de salida: tokens de verificación y de recuperación (solo hashes). */
public interface OneTimeTokenRepository {

  void save(OneTimeToken token);

  Optional<OneTimeToken> findByHash(String tokenHash, TokenPurpose purpose);

  /** Marca como usados los tokens vigentes del usuario para ese propósito. */
  void invalidateActive(UserId userId, TokenPurpose purpose, Instant now);
}
