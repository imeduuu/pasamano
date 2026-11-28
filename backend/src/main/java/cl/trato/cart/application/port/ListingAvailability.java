package cl.trato.cart.application.port;

import cl.trato.identity.domain.UserId;
import java.util.Optional;
import java.util.UUID;

/** Consulta de datos actuales de publicación requeridos por carrito; RF-CAR-04. */
public interface ListingAvailability {
  record Listing(
      UUID id, String title, UserId sellerId, long price, int availableStock, String status) {}

  Optional<Listing> findById(UUID listingId);
}
