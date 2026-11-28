package cl.trato.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.catalog.domain.Category;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingCondition;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** HU-15, CU-05: reglas de carrito y advertencias RF-CAR-04. */
class CartTest {

  private final UserId buyer = UserId.newId();
  private final UserId seller = UserId.newId();
  private final UserId anotherSeller = UserId.newId();

  @Test
  void addsListingSnapshotAndGroupsSellerSubtotalAndTotalInClp() {
    Cart cart = new Cart(buyer);
    Listing first = listing(seller, 2500, 5, ListingStatus.ACTIVA);
    Listing second = listing(seller, 1000, 3, ListingStatus.ACTIVA);
    Listing third = listing(anotherSeller, 2000, 2, ListingStatus.ACTIVA);

    cart.add(first, 2);
    cart.add(second, 1);
    cart.add(third, 1);

    assertThat(cart.items()).hasSize(3);
    assertThat(cart.items().getFirst().listingId()).isEqualTo(first.id());
    assertThat(cart.items().getFirst().sellerId()).isEqualTo(seller);
    assertThat(cart.items().getFirst().unitPrice()).isEqualTo(Money.of(2500));
    assertThat(cart.sellerSubtotals())
        .containsEntry(seller, Money.of(6000))
        .containsEntry(anotherSeller, Money.of(2000));
    assertThat(cart.total()).isEqualTo(Money.of(8000));
  }

  @Test
  void rejectsOwnListingRn02() {
    assertThatThrownBy(
            () -> new Cart(seller).add(listing(seller, 1000, 1, ListingStatus.ACTIVA), 1))
        .isInstanceOf(BusinessRuleViolationException.class)
        .hasMessageContaining("propia");
  }

  @Test
  void acceptsOnlyActiveListings() {
    for (ListingStatus status : ListingStatus.values()) {
      if (status != ListingStatus.ACTIVA) {
        assertThatThrownBy(() -> new Cart(buyer).add(listing(seller, 1000, 2, status), 1))
            .isInstanceOf(BusinessRuleViolationException.class);
      }
    }
  }

  @Test
  void addingSameListingIncreasesQuantityWithoutChangingCapturedPrice() {
    Cart cart = new Cart(buyer);
    Listing listing = listing(seller, 1500, 5, ListingStatus.ACTIVA);
    cart.add(listing, 1);
    Listing repriced = listingWithId(listing.id(), seller, 2000, 5, ListingStatus.ACTIVA);
    cart.add(repriced, 2);

    assertThat(cart.items()).hasSize(1);
    assertThat(cart.items().getFirst().quantity()).isEqualTo(3);
    assertThat(cart.items().getFirst().unitPrice()).isEqualTo(Money.of(1500));
  }

  @Test
  void rejectsQuantityAboveAvailableStockAndReportsMaximum() {
    assertThatThrownBy(() -> new Cart(buyer).add(listing(seller, 1000, 4, ListingStatus.ACTIVA), 5))
        .isInstanceOf(CartQuantityExceededException.class)
        .extracting("maximum")
        .isEqualTo(4);
  }

  @Test
  void reportsPriceChangedSoldOutPausedAndBlockedWarningsAgainstCurrentListing() {
    Cart cart = new Cart(buyer);
    Listing original = listing(seller, 1000, 5, ListingStatus.ACTIVA);
    cart.add(original, 1);

    Listing changedPrice = listingWithId(original.id(), seller, 1200, 5, ListingStatus.ACTIVA);
    assertThat(cart.warningsFor(changedPrice)).contains(CartWarning.PRICE_CHANGED);
    assertThat(
            cart.warningsFor(listingWithId(original.id(), seller, 1000, 0, ListingStatus.AGOTADA)))
        .containsExactly(CartWarning.SOLD_OUT);
    assertThat(
            cart.warningsFor(listingWithId(original.id(), seller, 1000, 5, ListingStatus.PAUSADA)))
        .containsExactly(CartWarning.PAUSED);
    assertThat(
            cart.warningsFor(
                listingWithId(original.id(), seller, 1000, 5, ListingStatus.BLOQUEADA)))
        .containsExactly(CartWarning.BLOCKED);
  }

  @Test
  void existingQuantityAlsoCannotExceedStock() {
    Cart cart = new Cart(buyer);
    Listing listing = listing(seller, 1000, 3, ListingStatus.ACTIVA);
    cart.add(listing, 2);

    assertThatThrownBy(() -> cart.add(listing, 2))
        .isInstanceOf(CartQuantityExceededException.class)
        .extracting("maximum")
        .isEqualTo(3);
  }

  void warningIsAbsentWhenPriceAndStatusMatchAndForUnknownListing() {
    Cart cart = new Cart(buyer);
    Listing listing = listing(seller, 1000, 5, ListingStatus.ACTIVA);
    cart.add(listing, 1);

    assertThat(cart.warningsFor(listing)).isEmpty();
    assertThat(cart.warningsFor(listing(seller, 1000, 5, ListingStatus.ACTIVA))).isEmpty();
  }

  private Listing listing(UserId owner, long price, int stock, ListingStatus status) {
    return listingWithId(UUID.randomUUID(), owner, price, stock, status);
  }

  private Listing listingWithId(
      UUID id, UserId owner, long price, int stock, ListingStatus status) {
    return Listing.rehydrate(
        id,
        "TÃ­tulo suficientemente largo",
        "DescripciÃ³n",
        Money.price(price),
        stock,
        0,
        ListingCondition.NUEVO,
        category(),
        "RM",
        "Santiago",
        owner,
        status,
        status == ListingStatus.BORRADOR ? java.util.List.of() : java.util.List.of("image"),
        null);
  }

  private Category category() {
    Category parent = Category.create("Hogar", 1, true, null);
    return Category.create("DecoraciÃ³n", 1, true, parent);
  }
}
