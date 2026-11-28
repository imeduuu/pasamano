package cl.trato.cart.domain;

import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Carrito de un comprador; HU-15 y CU-05. */
public final class Cart {

  private final UserId buyerId;
  private final Map<UUID, CartItem> items = new LinkedHashMap<>();

  public Cart(UserId buyerId) {
    this.buyerId = Objects.requireNonNull(buyerId, "buyerId");
  }

  /** Agrega o acumula unidades usando el precio vigente al primer agregado; RN-02. */
  public void add(Listing listing, int quantity) {
    Objects.requireNonNull(listing, "listing");
    if (listing.seller().equals(buyerId)) {
      throw new BusinessRuleViolationException(
          "CART_OWN_LISTING", "No puedes agregar una publicación propia (RN-02).");
    }
    if (listing.status() != ListingStatus.ACTIVA) {
      throw new BusinessRuleViolationException(
          "CART_LISTING_NOT_ACTIVE", "Solo puedes agregar publicaciones activas.");
    }
    if (quantity <= 0) {
      throw new BusinessRuleViolationException(
          "CART_INVALID_QUANTITY", "La cantidad debe ser positiva.");
    }
    CartItem existing = items.get(listing.id());
    int requested;
    try {
      requested = Math.addExact(existing == null ? 0 : existing.quantity(), quantity);
    } catch (ArithmeticException exception) {
      throw new CartQuantityExceededException(listing.availableStock());
    }
    if (requested > listing.availableStock()) {
      throw new CartQuantityExceededException(listing.availableStock());
    }
    if (existing == null) {
      items.put(
          listing.id(),
          new CartItem(listing.id(), listing.title(), listing.seller(), quantity, listing.price()));
    } else {
      items.put(listing.id(), existing.withQuantity(requested));
    }
  }

  public List<CartItem> items() {
    return List.copyOf(new ArrayList<>(items.values()));
  }

  /** Agrupa importes por vendedor, preservando el orden del primer agregado. */
  public Map<UserId, Money> sellerSubtotals() {
    Map<UserId, Money> subtotals = new LinkedHashMap<>();
    for (CartItem item : items.values()) {
      subtotals.merge(item.sellerId(), item.subtotal(), Money::plus);
    }
    return Map.copyOf(subtotals);
  }

  public Money total() {
    Money total = Money.zero();
    for (CartItem item : items.values()) {
      total = total.plus(item.subtotal());
    }
    return total;
  }

  /**
   * Calcula RF-CAR-04 sobre una publicación actual: precio se compara con el snapshot; agotada se
   * evalúa primero y tiene prioridad; pausada y bloqueada se informan según su estado actual.
   */
  public List<CartWarning> warningsFor(Listing currentListing) {
    Objects.requireNonNull(currentListing, "currentListing");
    CartItem item = items.get(currentListing.id());
    if (item == null) {
      return List.of();
    }
    List<CartWarning> warnings = new ArrayList<>();
    if (!item.unitPrice().equals(currentListing.price())) {
      warnings.add(CartWarning.PRICE_CHANGED);
    }
    switch (currentListing.status()) {
      case AGOTADA -> warnings.add(CartWarning.SOLD_OUT);
      case PAUSADA -> warnings.add(CartWarning.PAUSED);
      case BLOQUEADA -> warnings.add(CartWarning.BLOCKED);
      default -> {}
    }
    return List.copyOf(warnings);
  }
}
