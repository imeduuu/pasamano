package cl.trato.notifications.infrastructure.in.events;

import cl.trato.identity.domain.event.PasswordResetRequested;
import cl.trato.identity.domain.event.RegistrationAttemptedWithExistingEmail;
import cl.trato.identity.domain.event.VerificationEmailRequested;
import cl.trato.notifications.application.IdentityNotifications;
import cl.trato.notifications.infrastructure.config.NotificationsConfiguration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los eventos de identidad después del commit (ADR-008) y envía los correos de forma
 * asíncrona, sin bloquear la operación que los originó (RF-NOT-02).
 */
@Component
public class IdentityEventsListener {

  private final IdentityNotifications notifications;

  public IdentityEventsListener(IdentityNotifications notifications) {
    this.notifications = notifications;
  }

  @Async(NotificationsConfiguration.EMAIL_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(VerificationEmailRequested event) {
    notifications.on(event);
  }

  @Async(NotificationsConfiguration.EMAIL_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(PasswordResetRequested event) {
    notifications.on(event);
  }

  @Async(NotificationsConfiguration.EMAIL_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(RegistrationAttemptedWithExistingEmail event) {
    notifications.on(event);
  }
}
