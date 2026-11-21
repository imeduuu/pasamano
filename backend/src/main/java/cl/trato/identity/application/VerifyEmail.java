package cl.trato.identity.application;

import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.InvalidTokenException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/** CU-01 pasos 7 y 8: el enlace de un solo uso activa la cuenta (RF-AUT-02). */
public class VerifyEmail {

  private final UserRepository users;
  private final OneTimeTokenRepository tokens;
  private final TokenGenerator generator;
  private final Clock clock;

  public VerifyEmail(
      UserRepository users, OneTimeTokenRepository tokens, TokenGenerator generator, Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.generator = generator;
    this.clock = clock;
  }

  @Transactional
  public void execute(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw InvalidTokenException.notFound();
    }
    Instant now = clock.instant();
    OneTimeToken token =
        tokens
            .findByHash(generator.hash(rawToken), TokenPurpose.EMAIL_VERIFICATION)
            .orElseThrow(InvalidTokenException::notFound);
    OneTimeToken used = token.consume(now);
    User user = users.findById(token.userId()).orElseThrow(InvalidTokenException::notFound);

    user.verifyEmail(now);
    users.save(user);
    tokens.save(used);
  }
}
