package cl.trato.identity.domain.event;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Instant;

/**
 * Se creó una cuenta nueva (UsuarioRegistrado en el SRS §8.2).
 *
 * @param userId usuario
 * @param email correo
 * @param occurredAt momento (UTC)
 */
public record UserRegistered(UserId userId, Email email, Instant occurredAt)
    implements DomainEvent {}
