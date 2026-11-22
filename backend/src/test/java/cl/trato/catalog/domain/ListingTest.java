package cl.trato.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

/** RF-PUB-01, RN-01, RN-05, RN-10 y RN-12; ciclo de vida de la sección 8.6. */
class ListingTest {

  private static final Category ROOT = Category.create("Hogar", 1, true, null);
  private static final Category LEAF = Category.create("Cocina", 1, true, ROOT);

  private Listing draft(int stock) {
    return Listing.create(
        "Producto de prueba",
        "Descripción",
        Money.price(100),
        stock,
        0,
        ListingCondition.NUEVO,
        LEAF,
        "Región Metropolitana",
        "Santiago",
        UserId.newId());
  }

  @Test
  void creates_a_draft_with_valid_publication_data() {
    Listing listing = draft(3);

    assertThat(listing.status()).isEqualTo(ListingStatus.BORRADOR);
    assertThat(listing.availableStock()).isEqualTo(3);
    assertThat(listing.price()).isEqualTo(Money.price(100));
    assertThat(listing.images()).isEmpty();
  }

  @Test
  void enforces_listing_fields_and_stock_invariants() {
    assertThatThrownBy(() ->
            Listing.create(
                "corto",
                "",
                Money.price(100),
                1,
                0,
                ListingCondition.NUEVO,
                LEAF,
                "RM",
                "Santiago",
                UserId.newId()))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() ->
            Listing.create(
                "x".repeat(81),
                "x".repeat(4001),
                Money.price(100),
                1,
                0,
                ListingCondition.USADO,
                LEAF,
                "RM",
                "Santiago",
                UserId.newId()))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> draft(-1)).isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() ->
            Listing.create(
                "Producto de prueba",
                "",
                Money.price(100),
                1,
                2,
                ListingCondition.NUEVO,
                LEAF,
                "RM",
                "Santiago",
                UserId.newId()))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() ->
            Listing.create(
                "Producto de prueba",
                "",
                Money.price(100),
                1,
                0,
                ListingCondition.NUEVO,
                ROOT,
                "RM",
                "Santiago",
                UserId.newId()))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void publishes_only_with_one_to_eight_images_and_validates_each_transition() {
    Listing listing = draft(2);
    assertThatThrownBy(() -> listing.publish()).isInstanceOf(BusinessRuleViolationException.class);
    listing.addImage("img-1");
    listing.publish();
    assertThat(listing.status()).isEqualTo(ListingStatus.ACTIVA);
    assertThatThrownBy(listing::publish).isInstanceOf(BusinessRuleViolationException.class);

    listing.pause();
    assertThat(listing.status()).isEqualTo(ListingStatus.PAUSADA);
    assertThatThrownBy(listing::pause).isInstanceOf(BusinessRuleViolationException.class);
    listing.reactivate();
    assertThat(listing.status()).isEqualTo(ListingStatus.ACTIVA);

    listing.reserveStock(2);
    assertThat(listing.availableStock()).isZero();
    assertThat(listing.status()).isEqualTo(ListingStatus.AGOTADA);
    assertThatThrownBy(listing::pause).isInstanceOf(BusinessRuleViolationException.class);
    listing.restock(1);
    assertThat(listing.status()).isEqualTo(ListingStatus.ACTIVA);
    assertThat(listing.availableStock()).isEqualTo(1);
    assertThatThrownBy(() -> listing.restock(0)).isInstanceOf(DomainValidationException.class);

    listing.block();
    assertThat(listing.status()).isEqualTo(ListingStatus.BLOQUEADA);
    assertThatThrownBy(listing::reactivate).isInstanceOf(BusinessRuleViolationException.class);
    assertThatThrownBy(listing::pause).isInstanceOf(BusinessRuleViolationException.class);
    assertThatThrownBy(listing::delete).isInstanceOf(BusinessRuleViolationException.class);
    assertThatThrownBy(listing::block).isInstanceOf(BusinessRuleViolationException.class);
    assertThatThrownBy(listing::delete).isInstanceOf(BusinessRuleViolationException.class);

    Listing active = draft(1);
    active.addImage("img");
    active.publish();
    active.delete();
    assertThat(active.status()).isEqualTo(ListingStatus.ELIMINADA);
  }

  @Test
  void supports_other_valid_lifecycle_paths_and_rejects_invalid_ones() {
    Listing draft = draft(1);
    assertThatThrownBy(draft::pause).isInstanceOf(BusinessRuleViolationException.class);
    assertThatThrownBy(draft::reactivate).isInstanceOf(BusinessRuleViolationException.class);
    draft.delete();
    assertThat(draft.status()).isEqualTo(ListingStatus.ELIMINADA);
    assertThatThrownBy(draft::publish).isInstanceOf(BusinessRuleViolationException.class);

    Listing paused = draft(1);
    paused.addImage("img");
    paused.publish();
    paused.pause();
    paused.delete();
    assertThat(paused.status()).isEqualTo(ListingStatus.ELIMINADA);

    Listing soldOut = draft(1);
    soldOut.addImage("img");
    soldOut.publish();
    soldOut.reserveStock(1);
    soldOut.delete();
    assertThat(soldOut.status()).isEqualTo(ListingStatus.ELIMINADA);
  }

  @Test
  void image_collection_has_a_maximum_of_eight() {
    Listing listing = draft(1);
    for (int i = 0; i < 8; i++) {
      listing.addImage("img-" + i);
    }
    assertThat(listing.images()).hasSize(8);
    assertThatThrownBy(() -> listing.addImage("img-9"))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> listing.addImage(" ")).isInstanceOf(DomainValidationException.class);
  }
}
