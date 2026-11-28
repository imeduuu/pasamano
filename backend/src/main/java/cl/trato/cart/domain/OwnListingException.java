package cl.trato.cart.domain;

import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** HU-15/RN-02: un vendedor no puede comprar su propia publicación. */
public final class OwnListingException extends BusinessRuleViolationException {
  public OwnListingException() {
    super("OWN_LISTING", "No puedes agregar una publicación propia.");
  }

  @Override
  public ErrorCategory category() {
    return ErrorCategory.FORBIDDEN;
  }
}
