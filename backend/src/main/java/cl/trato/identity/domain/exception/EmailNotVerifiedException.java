package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** CU-02 2c: la cuenta aún no verifica su correo. */
public class EmailNotVerifiedException extends DomainException {

  public EmailNotVerifiedException() {
    super(
        "EMAIL_NOT_VERIFIED",
        ErrorCategory.FORBIDDEN,
        "Debes verificar tu correo antes de ingresar. Puedes pedir que te reenviemos el enlace.");
  }
}
