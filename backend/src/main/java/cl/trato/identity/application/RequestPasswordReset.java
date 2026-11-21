package cl.trato.identity.application;

import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.event.PasswordResetRequested;
import cl.trato.shared.application.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-16: envía un enlace de recuperación de 1 hora si la cuenta existe. La respuesta es siempre la
 * misma (RF-AUT-06).
 */
public class RequestPasswordReset {

  private final UserRepository users;
  private final OneTimeTokenIssuer tokenIssuer;
  private final DomainEventPublisher events;
  private final Clock clock;

  public RequestPasswordReset(
      UserRepository users,
      OneTimeTokenIssuer tokenIssuer,
      DomainEventPublisher events,
      Clock clock) {
    this.users = users;
    this.tokenIssuer = tokenIssuer;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public void execute(String rawEmail) {
    EmailInput.tryParse(rawEmail)
        .flatMap(users::findByEmail)
        .filter(user -> user.status() != UserStatus.SUSPENDED)
        .ifPresent(user -> {
          Instant now = clock.instant();
          OneTimeTokenIssuer.Issued token =
              tokenIssuer.issue(user.id(), TokenPurpose.PASSWORD_RESET, now);
          events.publish(
              List.of(
                  new PasswordResetRequested(
                      user.id(),
                      user.email(),
                      user.profile().name().value(),
                      token.rawToken(),
                      token.expiresAt(),
                      now)));
        });
  }
}
