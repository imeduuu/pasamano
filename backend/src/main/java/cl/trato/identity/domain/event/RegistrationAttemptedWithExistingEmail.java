package cl.trato.identity.domain.event;

import cl.trato.identity.domain.Email;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Instant;

/**
 * Alguien intentó registrarse con un correo ya registrado (CU-01 3b): se avisa al titular sin
 * revelar a quien intentó registrarse que el correo existe.
 *
 * @param email correo del titular
 * @param name nombre visible del titular
 * @param occurredAt momento (UTC)
 */
public record RegistrationAttemptedWithExistingEmail(Email email, String name, Instant occurredAt)
    implements DomainEvent {}
