package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** Token de verificación o de recuperación inexistente, vencido o ya usado. */
public class InvalidTokenException extends DomainException {

  public InvalidTokenException(String code, String message) {
    super(code, ErrorCategory.INVALID_INPUT, message);
  }

  public static InvalidTokenException notFound() {
    return new InvalidTokenException("TOKEN_INVALID", "El enlace no es válido.");
  }

  public static InvalidTokenException expired() {
    return new InvalidTokenException(
        "TOKEN_EXPIRED", "El enlace venció. Solicita uno nuevo para continuar.");
  }

  public static InvalidTokenException alreadyUsed() {
    return new InvalidTokenException(
        "TOKEN_ALREADY_USED", "Este enlace ya fue usado. Solicita uno nuevo si lo necesitas.");
  }
}
