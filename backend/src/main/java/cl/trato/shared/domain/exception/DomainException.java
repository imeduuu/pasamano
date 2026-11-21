package cl.trato.shared.domain.exception;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Excepción base del dominio. El {@code code} es estable (MAYÚSCULAS_CON_GUIONES_BAJOS) y la API lo
 * expone en el campo {@code code} de Problem Details; el mensaje se muestra al usuario en español.
 */
public abstract class DomainException extends RuntimeException {

  private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

  private final String code;
  private final ErrorCategory category;

  protected DomainException(String code, ErrorCategory category, String message) {
    super(message);
    if (code == null || !CODE_FORMAT.matcher(code).matches()) {
      throw new IllegalArgumentException("El código de error debe ser MAYUSCULAS_SNAKE: " + code);
    }
    this.code = code;
    this.category = Objects.requireNonNull(category, "category");
  }

  public String code() {
    return code;
  }

  public ErrorCategory category() {
    return category;
  }
}
