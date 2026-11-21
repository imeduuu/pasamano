package cl.trato.identity.application;

import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.PasswordPolicy;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.UserNotFoundException;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-AUT-07: cambia la contraseña estando autenticado, pidiendo la actual. Revoca los tokens de
 * refresco del usuario para cerrar las demás sesiones.
 */
public class ChangePassword {

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordPolicy passwordPolicy;
  private final PasswordHasher passwordHasher;
  private final Clock clock;

  public ChangePassword(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordPolicy passwordPolicy,
      PasswordHasher passwordHasher,
      Clock clock) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordPolicy = passwordPolicy;
    this.passwordHasher = passwordHasher;
    this.clock = clock;
  }

  @Transactional
  public void execute(ChangePasswordCommand command) {
    User user = users.findById(command.userId()).orElseThrow(UserNotFoundException::new);
    if (command.currentPassword() == null
        || !passwordHasher.matches(command.currentPassword(), user.passwordHash())) {
      throw new DomainValidationException(
          "CURRENT_PASSWORD_INCORRECT", "La contraseña actual no es correcta.");
    }
    passwordPolicy.validate(command.newPassword());
    Instant now = clock.instant();

    user.changePassword(passwordHasher.hash(command.newPassword()), now);
    users.save(user);
    refreshTokens.revokeAllForUser(user.id(), now);
  }
}
