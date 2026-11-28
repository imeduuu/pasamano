package cl.trato.cart.domain;

import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;

/** Snapshot de publicación, vendedor y precio al momento de agregar; HU-15. */
public record CartItem(
    UUID listingId, String title, UserId sellerId, int quantity, Money unitPrice) {

  public CartItem {
    Objects.requireNonNull(listingId, "listingId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(sellerId, "sellerId");
    Objects.requireNonNull(unitPrice, "unitPrice");
    if (quantity <= 0) {
      throw new IllegalArgumentException("La cantidad debe ser positiva.");
    }
  }

  CartItem withQuantity(int newQuantity) {
    return new CartItem(listingId, title, sellerId, newQuantity, unitPrice);
  }

  public Money subtotal() {
    return unitPrice.times(quantity);
  }
}
