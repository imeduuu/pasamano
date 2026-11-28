package cl.trato.cart.domain;

import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** HU-15: la publicación no está disponible para compra. */
public final class ListingUnavailableException extends BusinessRuleViolationException {
  public ListingUnavailableException() {
    super("LISTING_UNAVAILABLE", "La publicación no está disponible.");
  }

  @Override
  public ErrorCategory category() {
    return ErrorCategory.CONFLICT;
  }
}
