package cl.trato.identity.application;

import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.domain.UserId;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra la sesión revocando el token de refresco vigente y su familia (RF-AUT-05). Solo puede
 * revocar sesiones del propio usuario autenticado.
 */
public class Logout {

  private final RefreshTokenRepository refreshTokens;
  private final TokenGenerator generator;
  private final Clock clock;

  public Logout(RefreshTokenRepository refreshTokens, TokenGenerator generator, Clock clock) {
    this.refreshTokens = refreshTokens;
    this.generator = generator;
    this.clock = clock;
  }

  @Transactional
  public void execute(UserId authenticatedUser, String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByHash(generator.hash(rawRefreshToken))
        .filter(token -> token.userId().equals(authenticatedUser))
        .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
  }
}
