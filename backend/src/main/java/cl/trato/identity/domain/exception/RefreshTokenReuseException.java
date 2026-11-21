package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** RF-AUT-04: se presentó un token de refresco ya usado o revocado; se revoca su familia. */
public class RefreshTokenReuseException extends DomainException {

  public RefreshTokenReuseException() {
    super(
        "REFRESH_TOKEN_REUSED",
        ErrorCategory.AUTHENTICATION,
        "Tu sesión se cerró por seguridad. Inicia sesión nuevamente.");
  }
}
