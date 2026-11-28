package cl.trato.cart.domain;

import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** Informa el máximo total permitido por el stock actual; CU-05. */
public final class CartQuantityExceededException extends BusinessRuleViolationException {

  private final int maximum;

  public CartQuantityExceededException(int maximum) {
    super("INSUFFICIENT_STOCK", "La cantidad supera el stock disponible. Máximo: " + maximum + ".");
    this.maximum = maximum;
  }

  @Override
  public ErrorCategory category() {
    return ErrorCategory.CONFLICT;
  }

  public int maximum() {
    return maximum;
  }
}
