package cl.trato.identity.application;

import cl.trato.identity.application.port.AccessTokenIssuer;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import cl.trato.identity.domain.exception.SessionExpiredException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renueva el token de acceso con rotación del token de refresco (RF-AUT-04). Si llega un token ya
 * usado o revocado, se revoca toda su familia.
 */
public class RefreshSession {

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final TokenGenerator generator;
  private final AccessTokenIssuer accessTokens;
  private final Clock clock;

  public RefreshSession(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      TokenGenerator generator,
      AccessTokenIssuer accessTokens,
      Clock clock) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.generator = generator;
    this.accessTokens = accessTokens;
    this.clock = clock;
  }

  /** La revocación de la familia debe quedar guardada aunque se rechace la renovación. */
  @Transactional(noRollbackFor = {RefreshTokenReuseException.class, SessionExpiredException.class})
  public AuthSession execute(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new SessionExpiredException();
    }
    Instant now = clock.instant();
    RefreshToken current =
        refreshTokens
            .findByHash(generator.hash(rawRefreshToken))
            .orElseThrow(SessionExpiredException::new);

    String nextRaw = generator.generate();
    RefreshToken.Rotation rotation;
    try {
      rotation = current.rotate(generator.hash(nextRaw), now);
    } catch (RefreshTokenReuseException e) {
      refreshTokens.revokeFamily(current.familyId(), now);
      throw e;
    }

    User user = users.findById(current.userId()).filter(User::isActive).orElse(null);
    if (user == null) {
      refreshTokens.revokeAllForUser(current.userId(), now);
      throw new SessionExpiredException();
    }

    refreshTokens.save(rotation.used());
    refreshTokens.save(rotation.next());
    AccessTokenIssuer.IssuedAccessToken access = accessTokens.issue(user, now);
    return new AuthSession(
        user.id(), access.token(), access.expiresAt(), nextRaw, rotation.next().expiresAt(), now);
  }
}
