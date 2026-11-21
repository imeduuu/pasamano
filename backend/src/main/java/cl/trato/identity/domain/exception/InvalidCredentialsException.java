package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** Mensaje genérico de CU-02 2a: no revela si el correo existe. */
public class InvalidCredentialsException extends DomainException {

  public InvalidCredentialsException() {
    super("INVALID_CREDENTIALS", ErrorCategory.AUTHENTICATION, "Correo o contraseña incorrectos.");
  }
}
