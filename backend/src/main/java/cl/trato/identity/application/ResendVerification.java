package cl.trato.identity.application;

import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.event.VerificationEmailRequested;
import cl.trato.shared.application.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-01 8a y CU-02 2c: reenvía el enlace de verificación. Responde igual exista o no la cuenta,
 * para no revelar qué correos están registrados.
 */
public class ResendVerification {

  private final UserRepository users;
  private final OneTimeTokenIssuer tokenIssuer;
  private final DomainEventPublisher events;
  private final Clock clock;

  public ResendVerification(
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
        .filter(user -> user.status() == UserStatus.PENDING_VERIFICATION)
        .ifPresent(user -> {
          Instant now = clock.instant();
          OneTimeTokenIssuer.Issued token =
              tokenIssuer.issue(user.id(), TokenPurpose.EMAIL_VERIFICATION, now);
          events.publish(
              List.of(
                  new VerificationEmailRequested(
                      user.id(),
                      user.email(),
                      user.profile().name().value(),
                      token.rawToken(),
                      token.expiresAt(),
                      now)));
        });
  }
}
