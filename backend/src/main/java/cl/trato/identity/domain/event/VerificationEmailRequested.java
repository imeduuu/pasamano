package cl.trato.identity.domain.event;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Instant;

/**
 * Hay que enviar el enlace de verificación (CU-01 paso 5). El token en claro solo viaja en memoria
 * hasta el correo: en la base de datos se guarda su hash y nunca se imprime.
 *
 * @param userId usuario
 * @param email destinatario
 * @param name nombre visible para el saludo
 * @param token token en claro
 * @param expiresAt vencimiento del enlace
 * @param occurredAt momento (UTC)
 */
public record VerificationEmailRequested(
    UserId userId, Email email, String name, String token, Instant expiresAt, Instant occurredAt)
    implements DomainEvent {

  @Override
  public String toString() {
    return "VerificationEmailRequested[userId=" + userId + ", email=" + email + "]";
  }
}
