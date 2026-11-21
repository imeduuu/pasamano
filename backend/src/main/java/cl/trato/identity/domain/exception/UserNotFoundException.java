package cl.trato.identity.domain.exception;

import cl.trato.shared.domain.exception.ResourceNotFoundException;

/** El usuario no existe (por ejemplo, un token de acceso de una cuenta eliminada). */
public class UserNotFoundException extends ResourceNotFoundException {

  public UserNotFoundException() {
    super("USER_NOT_FOUND", "No encontramos la cuenta.");
  }
}
