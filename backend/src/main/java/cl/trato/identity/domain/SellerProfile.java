package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** HU-07 / CU-15 / RF-USR-02: perfil vendedor y aceptación de los términos de venta. */
public record SellerProfile(
    UserId userId,
    String storeName,
    String normalizedStoreName,
    String termsVersion,
    Instant termsAcceptedAt) {

  public SellerProfile {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(storeName, "storeName");
    Objects.requireNonNull(normalizedStoreName, "normalizedStoreName");
    Objects.requireNonNull(termsVersion, "termsVersion");
    Objects.requireNonNull(termsAcceptedAt, "termsAcceptedAt");
  }

  public static SellerProfile activate(
      UserId userId, String storeName, String termsVersion, Instant acceptedAt) {
    if (storeName == null || storeName.strip().length() < 3 || storeName.strip().length() > 40) {
      throw new DomainValidationException(
          "INVALID_STORE_NAME", "El nombre de tienda debe tener entre 3 y 40 caracteres.");
    }
    if (termsVersion == null || termsVersion.isBlank() || acceptedAt == null) {
      throw new DomainValidationException(
          "TERMS_ACCEPTANCE_REQUIRED", "Debes aceptar los términos de venta vigentes.");
    }
    String trimmed = storeName.strip();
    return new SellerProfile(
        userId, trimmed, trimmed.toLowerCase(Locale.ROOT), termsVersion, acceptedAt);
  }
}
