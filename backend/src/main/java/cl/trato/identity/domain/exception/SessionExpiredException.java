package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** La sesión de refresco no existe, venció o pertenece a una cuenta que ya no puede ingresar. */
public class SessionExpiredException extends DomainException {

  public SessionExpiredException() {
    super(
        "SESSION_EXPIRED",
        ErrorCategory.AUTHENTICATION,
        "Tu sesión expiró. Inicia sesión nuevamente.");
  }
}
