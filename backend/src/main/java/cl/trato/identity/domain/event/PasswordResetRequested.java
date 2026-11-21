package cl.trato.identity.domain.event;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Instant;

/**
 * Hay que enviar el enlace de recuperación de contraseña (CU-16). El token en claro nunca se
 * imprime.
 *
 * @param userId usuario
 * @param email destinatario
 * @param name nombre visible
 * @param token token en claro
 * @param expiresAt vencimiento del enlace
 * @param occurredAt momento (UTC)
 */
public record PasswordResetRequested(
    UserId userId, Email email, String name, String token, Instant expiresAt, Instant occurredAt)
    implements DomainEvent {

  @Override
  public String toString() {
    return "PasswordResetRequested[userId=" + userId + ", email=" + email + "]";
  }
}
