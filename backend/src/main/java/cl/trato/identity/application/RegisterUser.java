package cl.trato.identity.application;

import cl.trato.identity.application.port.ConsentRepository;
import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.Consent;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.LegalVersions;
import cl.trato.identity.domain.PasswordPolicy;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.event.RegistrationAttemptedWithExistingEmail;
import cl.trato.identity.domain.event.VerificationEmailRequested;
import cl.trato.shared.application.DomainEventPublisher;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-01 Registrarse (RF-AUT-01, RF-AUT-02). Si el correo ya existe no se crea nada y se avisa al
 * titular; la respuesta al visitante es la misma en ambos casos (3b).
 */
public class RegisterUser {

  private final UserRepository users;
  private final ConsentRepository consents;
  private final PasswordPolicy passwordPolicy;
  private final PasswordHasher passwordHasher;
  private final OneTimeTokenIssuer tokenIssuer;
  private final DomainEventPublisher events;
  private final LegalVersions legalVersions;
  private final Clock clock;

  public RegisterUser(
      UserRepository users,
      ConsentRepository consents,
      PasswordPolicy passwordPolicy,
      PasswordHasher passwordHasher,
      OneTimeTokenIssuer tokenIssuer,
      DomainEventPublisher events,
      LegalVersions legalVersions,
      Clock clock) {
    this.users = users;
    this.consents = consents;
    this.passwordPolicy = passwordPolicy;
    this.passwordHasher = passwordHasher;
    this.tokenIssuer = tokenIssuer;
    this.events = events;
    this.legalVersions = legalVersions;
    this.clock = clock;
  }

  @Transactional
  public void execute(RegisterUserCommand command) {
    Consent.requireAccepted(command.acceptedTerms());
    Email email = new Email(command.email());
    DisplayName name = new DisplayName(command.name());
    passwordPolicy.validate(command.password());
    Instant now = clock.instant();

    var existing = users.findByEmail(email);
    if (existing.isPresent()) {
      events.publish(
          List.of(
              new RegistrationAttemptedWithExistingEmail(
                  email, existing.get().profile().name().value(), now)));
      return;
    }

    User user =
        User.register(UserId.newId(), email, passwordHasher.hash(command.password()), name, now);
    List<DomainEvent> pending = new ArrayList<>(user.pullDomainEvents());
    users.save(user);
    consents.save(Consent.accept(user.id(), legalVersions, now));

    OneTimeTokenIssuer.Issued token =
        tokenIssuer.issue(user.id(), TokenPurpose.EMAIL_VERIFICATION, now);
    pending.add(
        new VerificationEmailRequested(
            user.id(), email, name.value(), token.rawToken(), token.expiresAt(), now));
    events.publish(pending);
  }
}
