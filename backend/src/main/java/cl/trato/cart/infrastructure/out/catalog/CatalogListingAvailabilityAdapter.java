package cl.trato.cart.infrastructure.out.catalog;

import cl.trato.cart.application.port.ListingAvailability;
import cl.trato.catalog.application.GetListingAvailability;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Consulta el catálogo por su caso de uso público, sin acceder a su infraestructura. */
@Component
class CatalogListingAvailabilityAdapter implements ListingAvailability {
  private final GetListingAvailability catalog;

  CatalogListingAvailabilityAdapter(GetListingAvailability catalog) {
    this.catalog = catalog;
  }

  @Override
  public Optional<Listing> findById(UUID listingId) {
    return catalog
        .find(listingId)
        .map(value ->
            new Listing(
                value.id(),
                value.title(),
                value.sellerId(),
                value.price(),
                value.stock(),
                value.status()));
  }
}
