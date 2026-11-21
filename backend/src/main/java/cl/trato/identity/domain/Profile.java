package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Perfil visible del usuario (RF-USR-01): nombre, foto, región y comuna. Los campos opcionales
 * vacíos se guardan como {@code null} (minimización de datos, RNF-LEG-01).
 *
 * @param name nombre visible
 * @param region región de Chile (texto libre, máx. 60)
 * @param commune comuna (texto libre, máx. 60)
 * @param photoUrl URL absoluta http(s) de la foto, máx. 500
 */
public record Profile(DisplayName name, String region, String commune, String photoUrl) {

  private static final int MAX_PLACE_LENGTH = 60;
  private static final int MAX_URL_LENGTH = 500;

  public Profile {
    if (name == null) {
      throw new DomainValidationException("INVALID_NAME", "El nombre es obligatorio.");
    }
    region = optionalPlace(region, "INVALID_REGION", "La región");
    commune = optionalPlace(commune, "INVALID_COMMUNE", "La comuna");
    photoUrl = optionalPhotoUrl(photoUrl);
  }

  public static Profile withName(DisplayName name) {
    return new Profile(name, null, null, null);
  }

  private static String optionalPlace(String raw, String code, String label) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.strip();
    if (trimmed.length() > MAX_PLACE_LENGTH) {
      throw new DomainValidationException(
          code, label + " puede tener como máximo " + MAX_PLACE_LENGTH + " caracteres.");
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
