package cl.trato.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ListingImages;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

/** Persistencia de publicaciones y RN-05 en PostgreSQL real mediante Testcontainers. */
@IntegrationTest
class ListingPersistenceIT {
  @Autowired ListingRepository listings;
  @Autowired ListingImages listingImages;
  @Autowired CategoryRepository categories;
  @Autowired UserRepository users;

  private Listing newListing(int stock) {
    String suffix = UserId.newId().value().toString().substring(0, 8);
    Category root = categories.save(Category.create("Hogar " + suffix, 1, true, null));
    Category leaf = categories.save(Category.create("Cocina " + suffix, 1, true, root));
    User seller =
        users.save(
            User.register(
                UserId.newId(),
                new Email("seller-" + UserId.newId().value() + "@ejemplo.cl"),
                new PasswordHash("$2a$12$hash"),
                new DisplayName("Vendedor"),
                Instant.now()));
    return Listing.create(
        "Producto para la cocina",
        "Descripción de prueba",
        Money.price(1500),
        stock,
        0,
        ListingCondition.NUEVO,
        leaf,
        "Región Metropolitana",
        "Santiago",
        seller.id());
  }

  @Test
  void saves_and_reads_listing_and_its_ordered_images() {
    Listing listing = newListing(3);
    listing.addImage("minio/listing/front.jpg");
    listing.addImage("minio/listing/back.jpg");

    Listing saved = listings.save(listing);
    Listing loaded = listings.findById(saved.id()).orElseThrow();

    assertThat(loaded.title()).isEqualTo(listing.title());
    assertThat(loaded.price()).isEqualTo(Money.price(1500));
    assertThat(loaded.seller()).isEqualTo(listing.seller());
    assertThat(loaded.category()).isEqualTo(listing.category());
    assertThat(loaded.images())
        .containsExactly("minio/listing/front.jpg", "minio/listing/back.jpg");
    assertThat(loaded.version()).isZero();
  }

  @Test
  void stores_original_and_thumbnail_references_for_uploaded_images() {
    Listing listing = listings.save(newListing(2));

    listingImages.add(listing.id(), "images/original.webp", "images/previews/custom.avif");

    assertThat(listingImages.findForListing(listing.id()))
        .containsExactly(
            new ListingImages.Reference("images/original.webp", "images/previews/custom.avif"));
  }

  @Test
  void rejects_two_updates_based_on_the_same_version() {
    Listing listing = newListing(4);
    listing.addImage("minio/listing/front.jpg");
    listing.publish();
    Listing saved = listings.save(listing);
    Listing first = listings.findById(saved.id()).orElseThrow();
    Listing stale = listings.findById(saved.id()).orElseThrow();

    first.reserveStock(1);
    listings.save(first);
    stale.reserveStock(2);

    assertThatThrownBy(() -> listings.save(stale))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }
}
