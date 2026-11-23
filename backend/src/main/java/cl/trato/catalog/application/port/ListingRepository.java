package cl.trato.catalog.application.port;

import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.domain.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de publicaciones del catálogo. */
public interface ListingRepository {
  Optional<Listing> findById(UUID id);

  List<Listing> findBySeller(UserId seller, ListingStatus status, int offset, int limit);

  long countActiveBySeller(UserId seller);

  long countBySeller(UserId seller, ListingStatus status);

  Listing save(Listing listing);
}
