package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;

/**
 * Nombre visible del usuario (RF-USR-01): de 2 a 60 caracteres, sin caracteres de control.
 *
 * @param value nombre sin espacios repetidos
 */
public record DisplayName(String value) {

  public static final int MIN_LENGTH = 2;
  public static final int MAX_LENGTH = 60;

  public DisplayName {
    String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
    int length = normalized.codePointCount(0, normalized.length());
    if (length < MIN_LENGTH
        || length > MAX_LENGTH
        || normalized.chars().anyMatch(Character::isISOControl)) {
      throw new DomainValidationException(
          "INVALID_NAME", "El nombre debe tener entre 2 y 60 caracteres.");
    }
    value = normalized;
  }
}
