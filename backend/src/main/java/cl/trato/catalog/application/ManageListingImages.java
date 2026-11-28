package cl.trato.catalog.application;

import cl.trato.catalog.application.port.ImageProcessor;
import cl.trato.catalog.application.port.ImageStorage;
import cl.trato.catalog.application.port.ListingImages;
import cl.trato.catalog.domain.Listing;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de imágenes de publicaciones (HU-09, RF-PUB-02, RN-12). */
public class ManageListingImages {
  private final ManageListings listings;
  private final ListingImages images;
  private final ImageProcessor processor;
  private final ImageStorage storage;

  public ManageListingImages(
      ManageListings listings,
      ListingImages images,
      ImageProcessor processor,
      ImageStorage storage) {
    this.listings = listings;
    this.images = images;
    this.processor = processor;
    this.storage = storage;
  }

  @Transactional
  public UUID add(UserId user, UUID listingId, String filename, byte[] content) {
    Listing listing = listings.getOwned(user, listingId);
    if (images.count(listingId) >= 8) {
      throw invalid("Una publicación admite como máximo 8 imágenes (RN-12).");
    }
    ImageProcessor.ProcessedImage processed = processor.process(filename, content);
    String key = "listings/" + listingId + "/" + UUID.randomUUID() + ".jpg";
    String thumbnail = "listings/" + listingId + "/" + UUID.randomUUID() + ".jpg";
    try {
      storage.store(key, processed.original(), processed.contentType());
      storage.store(thumbnail, processed.thumbnail(), processed.contentType());
      return images.add(listing.id(), key, thumbnail);
    } catch (RuntimeException exception) {
      deleteQuietly(thumbnail);
      deleteQuietly(key);
      throw exception;
    }
  }

  @Transactional
  public void remove(UserId user, UUID listingId, UUID imageId) {
    listings.getOwned(user, listingId);
    ListingImages.Reference image = images.remove(listingId, imageId);
    storage.deleteAfterCommit(image.key());
    storage.deleteAfterCommit(image.thumbnailKey());
  }

  @Transactional
  public void reorder(UserId user, UUID listingId, List<UUID> imageIds) {
    listings.getOwned(user, listingId);
    if (imageIds == null
        || imageIds.size() > 8
        || imageIds.stream().distinct().count() != imageIds.size()) {
      throw invalid("El orden debe incluir imágenes únicas de la publicación.");
    }
    images.reorder(listingId, imageIds);
  }

  private void deleteQuietly(String key) {
    try {
      storage.delete(key);
    } catch (RuntimeException ignored) {
      // Se conserva el error original; los adaptadores S3 hacen idempotente el borrado.
    }
  }

  private static DomainValidationException invalid(String message) {
    return new DomainValidationException("INVALID_LISTING_IMAGE", message);
  }
}
