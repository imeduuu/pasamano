package cl.trato.catalog.application.port;

import java.util.List;
import java.util.UUID;

/** Persistencia de identidad y orden de imágenes (HU-09, RF-PUB-02). */
public interface ListingImages {
  int count(UUID listingId);

  UUID add(UUID listingId, String key);

  String remove(UUID listingId, UUID imageId);

  void reorder(UUID listingId, List<UUID> imageIds);
}
