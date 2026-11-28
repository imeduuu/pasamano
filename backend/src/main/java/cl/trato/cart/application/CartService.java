package cl.trato.cart.application;

import cl.trato.cart.application.port.CartRepository;
import cl.trato.cart.application.port.ListingAvailability;
import cl.trato.cart.domain.Cart;
import cl.trato.cart.domain.CartItem;
import cl.trato.cart.domain.CartQuantityExceededException;
import cl.trato.cart.domain.ListingUnavailableException;
import cl.trato.cart.domain.OwnListingException;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de HU-15; el carrito siempre se resuelve por identidad autenticada. */
public class CartService {
  public record ItemView(
      UUID listingId,
      String title,
      int quantity,
      long unitPrice,
      long subtotal,
      boolean available,
      List<String> warnings) {}

  public record SellerView(UUID sellerId, List<ItemView> items, long subtotal) {}

  public record CartView(List<SellerView> sellers, long total, List<String> warnings) {}

  private final CartRepository carts;
  private final ListingAvailability listings;

  public CartService(CartRepository carts, ListingAvailability listings) {
    this.carts = carts;
    this.listings = listings;
  }

  @Transactional(readOnly = true)
  public CartView get(UserId buyer) {
    Cart cart = carts.findByBuyerId(buyer).orElseGet(() -> new Cart(buyer));
    Map<UserId, List<ItemView>> grouped = new LinkedHashMap<>();
    Map<UserId, Long> subtotals = new LinkedHashMap<>();
    List<String> allWarnings = new ArrayList<>();
    long total = 0;
    for (CartItem item : cart.items()) {
      var current = listings.findById(item.listingId()).orElse(null);
      List<String> warnings = new ArrayList<>();
      boolean available =
          current != null && "ACTIVA".equals(current.status()) && current.availableStock() > 0;
      if (current == null) {
        warnings.add("LISTING_UNAVAILABLE");
      } else {
        if (current.price() != item.unitPrice().amount()) {
          warnings.add("PRICE_CHANGED");
        }
        if (!available) {
          warnings.add(
              switch (current.status()) {
                case "AGOTADA" -> "SOLD_OUT";
                case "PAUSADA" -> "PAUSED";
                case "BLOQUEADA" -> "BLOCKED";
                default -> "LISTING_UNAVAILABLE";
              });
        }
      }
      allWarnings.addAll(warnings);
      long subtotal = item.subtotal().amount();
      grouped
          .computeIfAbsent(item.sellerId(), ignored -> new ArrayList<>())
          .add(
              new ItemView(
                  item.listingId(),
                  item.title(),
                  item.quantity(),
                  item.unitPrice().amount(),
                  subtotal,
                  available,
                  List.copyOf(warnings)));
      if (available) {
        total = Math.addExact(total, subtotal);
        subtotals.merge(item.sellerId(), subtotal, Math::addExact);
      }
    }
    List<SellerView> sellers =
        grouped.entrySet().stream()
            .map(entry ->
                new SellerView(
                    entry.getKey().value(),
                    List.copyOf(entry.getValue()),
                    subtotals.getOrDefault(entry.getKey(), 0L)))
            .toList();
    return new CartView(sellers, total, List.copyOf(allWarnings));
  }

  @Transactional
  public void put(UserId buyer, UUID listingId, int quantity) {
    var current =
        listings
            .findById(listingId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "LISTING_NOT_FOUND", "No encontramos la publicación."));
    if (current.sellerId().equals(buyer)) {
      throw new OwnListingException();
    }
    if (!"ACTIVA".equals(current.status()) || current.availableStock() <= 0) {
      throw new ListingUnavailableException();
    }
    if (quantity > current.availableStock()) {
      throw new CartQuantityExceededException(current.availableStock());
    }
    Cart cart = carts.findByBuyerId(buyer).orElseGet(() -> new Cart(buyer));
    cart.add(
        listingId,
        current.title(),
        current.sellerId(),
        current.availableStock(),
        Money.price(current.price()),
        quantity);
    carts.save(cart);
  }

  @Transactional
  public void delete(UserId buyer, UUID listingId) {
    Cart cart = carts.findByBuyerId(buyer).orElseGet(() -> new Cart(buyer));
    cart.remove(listingId);
    carts.save(cart);
  }
}
