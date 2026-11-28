package cl.trato.cart.domain;

import cl.trato.shared.domain.exception.BusinessRuleViolationException;

/** Informa el máximo total permitido por el stock actual; CU-05. */
public final class CartQuantityExceededException extends BusinessRuleViolationException {

  private final int maximum;

  public CartQuantityExceededException(int maximum) {
    super(
        "CART_STOCK_EXCEEDED", "La cantidad supera el stock disponible. Máximo: " + maximum + ".");
    this.maximum = maximum;
  }

  public int maximum() {
    return maximum;
  }
}
