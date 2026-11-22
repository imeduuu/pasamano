package cl.trato.identity.application;

import cl.trato.identity.application.port.AccessTokenIssuer;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.User;
import java.time.Instant;

/** Abre una sesión nueva: token de acceso y una familia nueva de tokens de refresco. */
public class SessionIssuer {

  private final RefreshTokenRepository refreshTokens;
  private final TokenGenerator generator;
  private final AccessTokenIssuer accessTokens;

  public SessionIssuer(
      RefreshTokenRepository refreshTokens,
      TokenGenerator generator,
      AccessTokenIssuer accessTokens) {
    this.refreshTokens = refreshTokens;
    this.generator = generator;
    this.accessTokens = accessTokens;
  }

  public AuthSession start(User user, Instant now) {
    String raw = generator.generate();
    RefreshToken refresh = RefreshToken.startFamily(user.id(), generator.hash(raw), now);
    refreshTokens.save(refresh);
    AccessTokenIssuer.IssuedAccessToken access = accessTokens.issue(user, now);
    return new AuthSession(
        user.id(), access.token(), access.expiresAt(), raw, refresh.expiresAt(), now);
  }
}
