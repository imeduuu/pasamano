package cl.trato.catalog.application.port;

import cl.trato.catalog.domain.Listing;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de publicaciones del catálogo. */
public interface ListingRepository {
  Optional<Listing> findById(UUID id);

  Listing save(Listing listing);
}
