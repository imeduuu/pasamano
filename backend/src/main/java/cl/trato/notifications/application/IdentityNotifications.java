package cl.trato.notifications.application;

import cl.trato.identity.domain.event.PasswordResetRequested;
import cl.trato.identity.domain.event.RegistrationAttemptedWithExistingEmail;
import cl.trato.identity.domain.event.VerificationEmailRequested;

/** Reacciona a los eventos de identidad enviando el correo correspondiente (RF-NOT-01). */
public class IdentityNotifications {

  private final EmailDelivery delivery;
  private final IdentityEmailTemplates templates;

  public IdentityNotifications(EmailDelivery delivery, IdentityEmailTemplates templates) {
    this.delivery = delivery;
    this.templates = templates;
  }

  public void on(VerificationEmailRequested event) {
    delivery.deliver(templates.verification(event.email().value(), event.name(), event.token()));
  }

  public void on(PasswordResetRequested event) {
    delivery.deliver(templates.passwordReset(event.email().value(), event.name(), event.token()));
  }

  public void on(RegistrationAttemptedWithExistingEmail event) {
    delivery.deliver(templates.registrationAttempt(event.email().value(), event.name()));
  }
}
