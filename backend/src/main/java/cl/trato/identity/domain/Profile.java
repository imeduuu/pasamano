package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Perfil visible del usuario (RF-USR-01): nombre, foto, región y comuna. Los campos opcionales
 * vacíos se guardan como {@code null} (minimización de datos, RNF-LEG-01).
 *
 * @param name nombre visible
 * @param region código CUT de dos dígitos de la región
 * @param commune código CUT de cinco dígitos de la comuna
 * @param photoUrl URL absoluta http(s) de la foto, máx. 500
 */
public record Profile(DisplayName name, String region, String commune, String photoUrl) {

  private static final java.util.regex.Pattern REGION_CODE =
      java.util.regex.Pattern.compile("\\d{2}");
  private static final java.util.regex.Pattern COMMUNE_CODE =
      java.util.regex.Pattern.compile("\\d{5}");
  private static final int MAX_URL_LENGTH = 500;

  public Profile {
    if (name == null) {
      throw new DomainValidationException("INVALID_NAME", "El nombre es obligatorio.");
    }
    region =
        optionalCode(
            region, REGION_CODE, "INVALID_REGION", "El código de región debe tener 2 dígitos.");
    commune =
        optionalCode(
            commune, COMMUNE_CODE, "INVALID_COMMUNE", "El código de comuna debe tener 5 dígitos.");
    if ((region == null) != (commune == null)) {
      throw new DomainValidationException("INVALID_LOCATION", "Debes indicar región y comuna.");
    }
    photoUrl = optionalPhotoUrl(photoUrl);
  }

  public static Profile withName(DisplayName name) {
    return new Profile(name, null, null, null);
  }

  private static String optionalCode(
      String raw, java.util.regex.Pattern pattern, String code, String message) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.strip();
    if (!pattern.matcher(trimmed).matches()) {
      throw new DomainValidationException(code, message);
    }
    return trimmed;
  }

  private static String optionalPhotoUrl(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.strip();
    if (trimmed.length() > MAX_URL_LENGTH || !isAbsoluteHttpUrl(trimmed)) {
      throw new DomainValidationException(
          "INVALID_PHOTO_URL", "La foto debe ser una URL http o https válida.");
    }
    return trimmed;
  }

  private static boolean isAbsoluteHttpUrl(String value) {
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme();
      return uri.getHost() != null && ("https".equals(scheme) || "http".equals(scheme));
    } catch (URISyntaxException e) {
      return false;
    }
  }
}
