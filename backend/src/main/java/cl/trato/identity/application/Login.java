package cl.trato.identity.application;

import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.exception.AccountLockedException;
import cl.trato.identity.domain.exception.AccountSuspendedException;
import cl.trato.identity.domain.exception.EmailNotVerifiedException;
import cl.trato.identity.domain.exception.InvalidCredentialsException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-02 Iniciar sesión (RF-AUT-03, RF-AUT-08, RN-17). El estado de la cuenta solo se revela tras
 * comprobar la contraseña, salvo el bloqueo temporal, que el SRS pide informar (2b).
 */
public class Login {

  private final UserRepository users;
  private final PasswordHasher passwordHasher;
  private final SessionIssuer sessions;
  private final Clock clock;

  public Login(
      UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessions, Clock clock) {
    this.users = users;
    this.passwordHasher = passwordHasher;
    this.sessions = sessions;
    this.clock = clock;
  }

  /** El contador de intentos fallidos debe quedar guardado aunque se rechace el ingreso. */
  @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
  public AuthSession execute(LoginCommand command) {
    Optional<User> found = EmailInput.tryParse(command.email()).flatMap(users::findByEmail);
    if (found.isEmpty()) {
      passwordHasher.matchesDummy(command.password());
      throw new InvalidCredentialsException();
    }
    User user = found.get();
    Instant now = clock.instant();

    if (user.isLocked(now)) {
      throw new AccountLockedException(user.lockedUntil());
    }
    if (!passwordHasher.matches(command.password(), user.passwordHash())) {
      user.registerFailedLogin(now);
      users.save(user);
      if (user.isLocked(now)) {
        throw new AccountLockedException(user.lockedUntil());
      }
      throw new InvalidCredentialsException();
    }
    if (user.status() == UserStatus.SUSPENDED) {
      throw new AccountSuspendedException();
    }
    if (user.status() == UserStatus.PENDING_VERIFICATION) {
      throw new EmailNotVerifiedException();
    }

    user.registerSuccessfulLogin();
    users.save(user);
    return sessions.start(user, now);
  }
}
