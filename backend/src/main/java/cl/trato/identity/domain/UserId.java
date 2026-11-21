package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un usuario.
 *
 * @param value UUID del usuario
 */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId newId() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId of(String raw) {
    try {
      return new UserId(UUID.fromString(raw));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new DomainValidationException("INVALID_USER_ID", "El identificador no es válido.");
    }
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
