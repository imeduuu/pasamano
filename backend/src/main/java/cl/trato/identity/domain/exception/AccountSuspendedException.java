package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** CU-02 2d y RN-17: un usuario suspendido no puede iniciar sesión. */
public class AccountSuspendedException extends DomainException {

  public AccountSuspendedException() {
    super(
        "ACCOUNT_SUSPENDED",
        ErrorCategory.FORBIDDEN,
        "Tu cuenta está suspendida. Escríbenos si crees que es un error.");
  }
}
