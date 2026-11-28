package cl.trato.catalog.application;

import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Listing;
import cl.trato.identity.domain.UserId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta interna pública del catálogo para validar carritos, sin exponer persistencia
 * (RF-CAR-04).
 */
public class GetListingAvailability {
  public record Availability(
      UUID id, String title, UserId sellerId, long price, int stock, String status) {}

  private final ListingRepository listings;

  public GetListingAvailability(ListingRepository listings) {
    this.listings = listings;
  }

  @Transactional(readOnly = true)
  public Optional<Availability> find(UUID listingId) {
    return listings.findById(listingId).map(GetListingAvailability::view);
  }

  private static Availability view(Listing listing) {
    return new Availability(
        listing.id(),
        listing.title(),
        listing.seller(),
        listing.price().amount(),
        listing.availableStock(),
        listing.status().name());
  }
}
