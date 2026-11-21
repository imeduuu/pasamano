package cl.trato.identity.domain;

/**
 * Versiones vigentes de los términos y condiciones y de la política de privacidad.
 *
 * @param termsVersion versión de los términos
 * @param privacyVersion versión de la política de privacidad
 */
public record LegalVersions(String termsVersion, String privacyVersion) {

  public LegalVersions {
    if (termsVersion == null || termsVersion.isBlank()) {
      throw new IllegalArgumentException("La versión de los términos es obligatoria");
    }
    if (privacyVersion == null || privacyVersion.isBlank()) {
      throw new IllegalArgumentException("La versión de la política de privacidad es obligatoria");
    }
  }
}
