package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electrónico normalizado (minúsculas, sin espacios). Es el identificador de inicio de
 * sesión, por lo que dos correos que solo difieren en mayúsculas son el mismo.
 *
 * @param value correo normalizado
 */
public record Email(String value) {

  private static final int MAX_LENGTH = 254;
  private static final Pattern FORMAT =
      Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$", Pattern.UNICODE_CHARACTER_CLASS);

  public Email {
    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()
        || normalized.length() > MAX_LENGTH
        || !FORMAT.matcher(normalized).matches()) {
      throw new DomainValidationException("INVALID_EMAIL", "El correo electrónico no es válido.");
    }
    value = normalized;
  }

  /** Versión enmascarada para logs, sin exponer el dato personal (RNF-OBS-01). */
  public String masked() {
    int at = value.indexOf('@');
    String local = value.substring(0, at);
    String visible = local.length() > 1 ? local.charAt(0) + "***" : "*";
    return visible + value.substring(at);
  }

  @Override
  public String toString() {
    return masked();
  }
}
