package cl.trato.identity.application;

import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.PasswordPolicy;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.InvalidTokenException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-16: define la nueva contraseña con el enlace de un solo uso y revoca todos los tokens de
 * refresco del usuario.
 */
public class ResetPassword {

  private final UserRepository users;
  private final OneTimeTokenRepository tokens;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordPolicy passwordPolicy;
  private final PasswordHasher passwordHasher;
  private final TokenGenerator generator;
  private final Clock clock;

  public ResetPassword(
      UserRepository users,
      OneTimeTokenRepository tokens,
      RefreshTokenRepository refreshTokens,
      PasswordPolicy passwordPolicy,
      PasswordHasher passwordHasher,
      TokenGenerator generator,
      Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.refreshTokens = refreshTokens;
    this.passwordPolicy = passwordPolicy;
    this.passwordHasher = passwordHasher;
    this.generator = generator;
    this.clock = clock;
  }

  @Transactional
  public void execute(ResetPasswordCommand command) {
    passwordPolicy.validate(command.newPassword());
    if (command.token() == null || command.token().isBlank()) {
      throw InvalidTokenException.notFound();
    }
    Instant now = clock.instant();
    OneTimeToken token =
        tokens
            .findByHash(generator.hash(command.token()), TokenPurpose.PASSWORD_RESET)
            .orElseThrow(InvalidTokenException::notFound);
    OneTimeToken used = token.consume(now);
    User user = users.findById(token.userId()).orElseThrow(InvalidTokenException::notFound);

    user.changePassword(passwordHasher.hash(command.newPassword()), now);
    users.save(user);
    tokens.save(used);
    refreshTokens.revokeAllForUser(user.id(), now);
  }
}
