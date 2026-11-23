package cl.trato.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Category;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingCondition;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.geo.application.port.RegionRepository;
import cl.trato.geo.domain.Commune;
import cl.trato.geo.domain.Region;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.ConflictException;
import cl.trato.shared.domain.exception.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-08/HU-10: reglas de acceso y validación de publicaciones. */
class ManageListingsTest {
  private final ListingRepository listings = mock(ListingRepository.class);
  private final CategoryRepository categories = mock(CategoryRepository.class);
  private final RegionRepository regions = mock(RegionRepository.class);
  private final SellerProfileRepository sellers = mock(SellerProfileRepository.class);
  private final ManageListings useCase = new ManageListings(listings, categories, regions, sellers);
  private final UserId seller = UserId.newId();
  private final UUID rootId = UUID.randomUUID();
  private final Category leaf = new Category(UUID.randomUUID(), "Casa", "casa", 1, true, rootId);
  private final ManageListings.Command command =
      new ManageListings.Command(
          "Mesa de madera usada",
          "Descripción",
          1500,
          2,
          ListingCondition.USADO,
          leaf.id(),
          "13",
          "13101");

  @BeforeEach
  void setup() {
    when(sellers.findByUserId(seller))
        .thenReturn(Optional.of(SellerProfile.activate(seller, "Mi tienda", "v1", Instant.now())));
    when(categories.findById(leaf.id())).thenReturn(Optional.of(leaf));
    when(regions.findAll())
        .thenReturn(
            List.of(
                new Region(
                    "13", "Metropolitana", List.of(new Commune("13101", "Santiago", "13")))));
    when(listings.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          Listing item = invocation.getArgument(0);
          return Listing.rehydrate(
              item.id(),
              item.title(),
              item.description(),
              item.price(),
              item.stockTotal(),
              item.stockReserved(),
              item.condition(),
              item.category(),
              item.region(),
              item.commune(),
              item.seller(),
              item.status(),
              item.images(),
              item.version() == null ? 0L : item.version() + 1);
        });
  }

  @Test
  void creates_a_draft_after_profile_category_and_location_validation() {
    Listing created = useCase.create(seller, command);
    assertThat(created.status()).isEqualTo(ListingStatus.BORRADOR);
    assertThat(created.version()).isZero();
  }

  @Test
  void refuses_creation_without_seller_profile_with_stable_code() {
    when(sellers.findByUserId(seller)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.create(seller, command))
        .isInstanceOfSatisfying(
            ListingForbiddenException.class,
            error -> assertThat(error.code()).isEqualTo("SELLER_PROFILE_REQUIRED"));
  }

  @Test
  void refuses_inactive_or_parent_category_and_invalid_commune() {
    when(categories.findById(leaf.id()))
        .thenReturn(Optional.of(new Category(leaf.id(), "Casa", "casa", 1, false, rootId)));
    assertThatThrownBy(() -> useCase.create(seller, command)).isInstanceOf(DomainException.class);
    when(categories.findById(leaf.id())).thenReturn(Optional.of(leaf));
    ManageListings.Command wrongLocation =
        new ManageListings.Command(
            command.title(),
            command.description(),
            command.price(),
            command.stock(),
            command.condition(),
            command.categoryId(),
            "01",
            "13101");
    assertThatThrownBy(() -> useCase.create(seller, wrongLocation))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void only_owner_can_update_and_version_must_match() {
    Listing saved = createSaved();
    UserId other = UserId.newId();
    when(listings.findById(saved.id())).thenReturn(Optional.of(saved));
    assertThatThrownBy(() -> useCase.update(other, saved.id(), 0, command))
        .isInstanceOf(ListingForbiddenException.class);
    assertThatThrownBy(() -> useCase.update(seller, saved.id(), 1, command))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updates_and_soft_deletes_owned_listing() {
    Listing saved = createSaved();
    when(listings.findById(saved.id())).thenReturn(Optional.of(saved));
    assertThat(useCase.update(seller, saved.id(), 0, command).version()).isEqualTo(1);
    useCase.delete(seller, saved.id(), 0);
  }

  @Test
  void missing_listing_is_not_found_and_stale_delete_conflicts() {
    UUID missing = UUID.randomUUID();
    assertThatThrownBy(() -> useCase.delete(seller, missing, 0))
        .isInstanceOf(DomainException.class);
    Listing saved = createSaved();
    when(listings.findById(saved.id())).thenReturn(Optional.of(saved));
    assertThatThrownBy(() -> useCase.delete(seller, saved.id(), 3))
        .isInstanceOf(ConflictException.class);
  }

  private Listing createSaved() {
    return Listing.rehydrate(
        UUID.randomUUID(),
        command.title(),
        command.description(),
        cl.trato.shared.domain.Money.price(command.price()),
        command.stock(),
        0,
        command.condition(),
        leaf,
        command.region(),
        command.commune(),
        seller,
        ListingStatus.BORRADOR,
        List.of(),
        0L);
  }
}
