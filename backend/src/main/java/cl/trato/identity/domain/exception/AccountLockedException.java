package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;
import java.time.Instant;

/** CU-02 2b: cuenta bloqueada por 15 minutos tras 5 intentos fallidos (RF-AUT-08). */
public class AccountLockedException extends DomainException {

  private final transient Instant lockedUntil;

  public AccountLockedException(Instant lockedUntil) {
    super(
        "ACCOUNT_LOCKED",
        ErrorCategory.LOCKED,
        "Bloqueamos el inicio de sesión por 15 minutos tras 5 intentos fallidos."
            + " Intenta más tarde o recupera tu contraseña.");
    this.lockedUntil = lockedUntil;
  }

  public Instant lockedUntil() {
    return lockedUntil;
  }
}
