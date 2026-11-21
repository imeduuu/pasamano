package cl.trato.identity.domain;

import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import cl.trato.identity.domain.exception.SessionExpiredException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de refresco opaco (ADR-002). Cada inicio de sesión abre una familia; cada renovación marca
 * el token como usado y emite el siguiente de la misma familia. Presentar un token ya usado o
 * revocado es una reutilización: la aplicación revoca la familia completa (RF-AUT-04).
 *
 * @param id identificador
 * @param userId titular
 * @param familyId familia de rotación
 * @param tokenHash hash SHA-256 del token en claro
 * @param expiresAt vencimiento (7 días)
 * @param createdAt emisión
 * @param usedAt momento en que se rotó, o {@code null}
 * @param revokedAt momento en que se revocó, o {@code null}
 */
public record RefreshToken(
    UUID id,
    UserId userId,
    UUID familyId,
    String tokenHash,
    Instant expiresAt,
    Instant createdAt,
    Instant usedAt,
    Instant revokedAt) {

  /** RF-AUT-03: vigencia del token de refresco. */
  public static final Duration VALIDITY = Duration.ofDays(7);

  /**
   * Resultado de una rotación.
   *
   * @param used token presentado, ahora marcado como usado
   * @param next token nuevo de la misma familia
   */
  public record Rotation(RefreshToken used, RefreshToken next) {}

  public RefreshToken {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(familyId, "familyId");
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(createdAt, "createdAt");
    if (tokenHash == null || tokenHash.isBlank()) {
      throw new IllegalArgumentException("El hash del token es obligatorio");
    }
  }

  public static RefreshToken startFamily(UserId userId, String tokenHash, Instant now) {
    return issue(userId, UUID.randomUUID(), tokenHash, now);
  }

  private static RefreshToken issue(UserId userId, UUID familyId, String hash, Instant now) {
    return new RefreshToken(
        UUID.randomUUID(), userId, familyId, hash, now.plus(VALIDITY), now, null, null);
  }

  public boolean isActive(Instant now) {
    return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
  }

  /**
   * Rota el token.
   *
   * @throws RefreshTokenReuseException si el token ya se usó o fue revocado
   * @throws SessionExpiredException si el token venció
   */
  public Rotation rotate(String nextTokenHash, Instant now) {
    if (usedAt != null || revokedAt != null) {
      throw new RefreshTokenReuseException();
    }
    if (!now.isBefore(expiresAt)) {
      throw new SessionExpiredException();
    }
    RefreshToken used =
        new RefreshToken(id, userId, familyId, tokenHash, expiresAt, createdAt, now, revokedAt);
    return new Rotation(used, issue(userId, familyId, nextTokenHash, now));
  }

  public RefreshToken revoke(Instant now) {
    if (revokedAt != null) {
      return this;
    }
    return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, createdAt, usedAt, now);
  }
}
