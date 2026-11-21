package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro del consentimiento informado al crear la cuenta (CU-01 paso 4, RNF-LEG-01).
 *
 * @param id identificador
 * @param userId titular
 * @param termsVersion versión de los términos aceptada
 * @param privacyVersion versión de la política de privacidad aceptada
 * @param acceptedAt momento de la aceptación (UTC)
 */
public record Consent(
    UUID id, UserId userId, String termsVersion, String privacyVersion, Instant acceptedAt) {

  public Consent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(acceptedAt, "acceptedAt");
    new LegalVersions(termsVersion, privacyVersion);
  }

  public static Consent accept(UserId userId, LegalVersions versions, Instant now) {
    return new Consent(
        UUID.randomUUID(), userId, versions.termsVersion(), versions.privacyVersion(), now);
  }

  /** Los términos y la política de privacidad deben aceptarse de forma explícita. */
  public static void requireAccepted(boolean accepted) {
    if (!accepted) {
      throw new DomainValidationException(
          "TERMS_NOT_ACCEPTED", "Debes aceptar los términos y la política de privacidad.");
    }
  }
}
