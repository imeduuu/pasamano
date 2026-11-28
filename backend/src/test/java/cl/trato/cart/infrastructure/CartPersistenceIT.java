package cl.trato.cart.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.cart.application.port.CartRepository;
import cl.trato.cart.application.port.ListingAvailability;
import cl.trato.cart.domain.Cart;
import cl.trato.cart.domain.CartItem;
import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Category;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingCondition;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.support.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Persistencia PostgreSQL del carrito y sus snapshots (RF-CAR-05; Redis en F2 por RF-CAR-06). */
@IntegrationTest
@Transactional
class CartPersistenceIT {
  @Autowired CartRepository carts;
  @Autowired UserRepository users;
  @Autowired CategoryRepository categories;
  @Autowired ListingRepository listings;
  @Autowired ListingAvailability listingAvailability;

  @Test
  void persists_one_cart_per_buyer_and_restores_item_snapshots() {
    UserId buyer = createUser();
    UUID listingId = UUID.randomUUID();
    Cart cart =
        Cart.rehydrate(
            buyer,
            java.util.List.of(
                new CartItem(listingId, "Cámara usada", createUser(), 2, Money.price(12500))));

    carts.save(cart);
    Cart restored = carts.findByBuyerId(buyer).orElseThrow();

    assertThat(restored.buyerId()).isEqualTo(buyer);
    assertThat(restored.items())
        .containsExactly(
            new CartItem(
                listingId,
                "Cámara usada",
                cart.items().getFirst().sellerId(),
                2,
                Money.price(12500)));
    assertThat(restored.total()).isEqualTo(Money.of(25000));
  }

  @Test
  void replaces_items_when_saving_the_same_buyer_cart() {
    UserId buyer = createUser();
    Cart first =
        Cart.rehydrate(
            buyer,
            java.util.List.of(
                new CartItem(
                    UUID.randomUUID(), "Producto inicial", createUser(), 1, Money.price(100))));
    carts.save(first);
    Cart updated = Cart.rehydrate(buyer, java.util.List.of());

    carts.save(updated);

    assertThat(carts.findByBuyerId(buyer).orElseThrow().items()).isEmpty();
  }

  @Test
  void reads_current_price_stock_and_status_through_catalog_application() {
    UserId seller = createUser();
    String suffix = seller.value().toString().substring(0, 8);
    Category root = categories.save(Category.create("Hogar " + suffix, 1, true, null));
    Category leaf = categories.save(Category.create("Cocina " + suffix, 1, true, root));
    Listing listing =
        Listing.create(
            "Producto para validar carrito",
            "Descripción de integración",
            Money.price(1800),
            4,
            1,
            ListingCondition.NUEVO,
            leaf,
            "Región Metropolitana",
            "Santiago",
            seller);
    listing.addImage("images/cart-it.webp");
    listing.publish();
    Listing saved = listings.save(listing);
    saved.pause();
    listings.save(saved);

    ListingAvailability.Listing current = listingAvailability.findById(saved.id()).orElseThrow();

    assertThat(current.title()).isEqualTo(saved.title());
    assertThat(current.sellerId()).isEqualTo(seller);
    assertThat(current.price()).isEqualTo(1800);
    assertThat(current.availableStock()).isEqualTo(3);
    assertThat(current.status()).isEqualTo("PAUSADA");
  }

  private UserId createUser() {
    UserId id = UserId.newId();
    return users
        .save(
            User.register(
                id,
                new Email("cart-" + id + "@ejemplo.cl"),
                new PasswordHash("$2a$12$hash"),
                new DisplayName("Comprador"),
                Instant.now()))
        .id();
  }
}
