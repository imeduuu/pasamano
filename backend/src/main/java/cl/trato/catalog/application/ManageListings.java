package cl.trato.catalog.application;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Category;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingCondition;
import cl.trato.geo.application.port.RegionRepository;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.DomainValidationException;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Alta, edición y eliminación lógica de publicaciones (HU-08, HU-10; RF-PUB-01/03/04/06). */
public class ManageListings {
  public record Command(
      String title,
      String description,
      long price,
      int stock,
      ListingCondition condition,
      UUID categoryId,
      String region,
      String commune) {}

  private final ListingRepository listings;
  private final CategoryRepository categories;
  private final RegionRepository regions;
  private final SellerProfileRepository sellers;

  public ManageListings(
      ListingRepository listings,
      CategoryRepository categories,
      RegionRepository regions,
      SellerProfileRepository sellers) {
    this.listings = listings;
    this.categories = categories;
    this.regions = regions;
    this.sellers = sellers;
  }

  @Transactional
  public Listing create(UserId user, Command command) {
    requireSeller(user);
    Category category = requireCategory(command.categoryId());
    requireLocation(command.region(), command.commune());
    return listings.save(
        Listing.create(
            command.title(),
            command.description(),
            Money.price(command.price()),
            command.stock(),
            0,
            command.condition(),
            category,
            command.region(),
            command.commune(),
            user));
  }

  @Transactional
  public Listing update(UserId user, UUID id, long version, Command command) {
    Listing current = requireListing(id);
    requireOwner(current, user);
    if (!Long.valueOf(version).equals(current.version())) {
      throw new cl.trato.shared.domain.exception.ConflictException(
          "CONCURRENT_MODIFICATION", "La publicación cambió. Recarga antes de editar.");
    }
    Category category = requireCategory(command.categoryId());
    requireLocation(command.region(), command.commune());
    return listings.save(
        current.update(
            command.title(),
            command.description(),
            Money.price(command.price()),
            command.stock(),
            command.condition(),
            category,
            command.region(),
            command.commune()));
  }

  @Transactional
  public void delete(UserId user, UUID id, long version) {
    Listing current = requireListing(id);
    requireOwner(current, user);
    if (!Long.valueOf(version).equals(current.version())) {
      throw new cl.trato.shared.domain.exception.ConflictException(
          "CONCURRENT_MODIFICATION", "La publicación cambió. Recarga antes de eliminar.");
    }
    current.delete();
    listings.save(current);
  }

  public Listing getOwned(UserId user, UUID id) {
    Listing listing = requireListing(id);
    requireOwner(listing, user);
    return listing;
  }

  private void requireSeller(UserId user) {
    if (sellers.findByUserId(user).isEmpty()) {
      throw new ListingForbiddenException(
          "SELLER_PROFILE_REQUIRED", "Debes activar tu perfil vendedor para publicar.");
    }
  }

  private Category requireCategory(UUID id) {
    Category category =
        categories
            .findById(id)
            .orElseThrow(() ->
                new DomainValidationException(
                    "INVALID_LISTING_CATEGORY", "La categoría indicada no existe."));
    if (!category.active() || category.parentId() == null) {
      throw new DomainValidationException(
          "INVALID_LISTING_CATEGORY", "Debes elegir una categoría hoja activa.");
    }
    return category;
  }

  private void requireLocation(String regionCode, String communeCode) {
    boolean valid =
        regions.findAll().stream()
            .filter(region -> region.code().equals(regionCode))
            .anyMatch(region -> region.contains(communeCode));
    if (!valid) {
      throw new DomainValidationException(
          "INVALID_LISTING_LOCATION", "La comuna debe pertenecer a la región indicada.");
    }
  }

  private Listing requireListing(UUID id) {
    return listings
        .findById(id)
        .filter(listing -> listing.status() != cl.trato.catalog.domain.ListingStatus.ELIMINADA)
        .orElseThrow(() ->
            new ResourceNotFoundException("LISTING_NOT_FOUND", "No se encontró la publicación."));
  }

  private static void requireOwner(Listing listing, UserId user) {
    if (!listing.seller().equals(user)) {
      throw new ListingForbiddenException(
          "LISTING_NOT_OWNED", "Solo puedes modificar tus propias publicaciones.");
    }
  }
}
