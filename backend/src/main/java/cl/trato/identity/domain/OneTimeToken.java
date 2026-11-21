package cl.trato.identity.domain;

import cl.trato.identity.domain.exception.InvalidTokenException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de un solo uso para verificar el correo o recuperar la contraseña. Solo se guarda el hash
 * del valor enviado por correo.
 *
 * @param id identificador
 * @param userId titular
 * @param purpose propósito, que define la vigencia
 * @param tokenHash hash SHA-256 del token en claro
 * @param expiresAt vencimiento
 * @param usedAt momento de uso, o {@code null} si no se ha usado
 * @param createdAt emisión
 */
public record OneTimeToken(
    UUID id,
    UserId userId,
    TokenPurpose purpose,
    String tokenHash,
    Instant expiresAt,
    Instant usedAt,
    Instant createdAt) {

  public OneTimeToken {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(purpose, "purpose");
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(createdAt, "createdAt");
    if (tokenHash == null || tokenHash.isBlank()) {
      throw new IllegalArgumentException("El hash del token es obligatorio");
    }
  }

  public static OneTimeToken issue(
      UserId userId, TokenPurpose purpose, String tokenHash, Instant now) {
    return new OneTimeToken(
        UUID.randomUUID(), userId, purpose, tokenHash, now.plus(purpose.validity()), null, now);
  }

  /** Marca el token como usado; falla si ya se usó o venció. */
  public OneTimeToken consume(Instant now) {
    if (usedAt != null) {
      throw InvalidTokenException.alreadyUsed();
    }
    if (!now.isBefore(expiresAt)) {
      throw InvalidTokenException.expired();
    }
    return new OneTimeToken(id, userId, purpose, tokenHash, expiresAt, now, createdAt);
  }
}
