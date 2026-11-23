package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.application.port.ListingImages;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador JPA de la colección de imágenes (HU-09, RF-PUB-02). */
@Component
class JpaListingImagesAdapter implements ListingImages {
  private final ListingJpaRepository listings;
  private final ListingImageJpaRepository images;

  JpaListingImagesAdapter(ListingJpaRepository listings, ListingImageJpaRepository images) {
    this.listings = listings;
    this.images = images;
  }

  @Override
  public int count(UUID listingId) {
    if (listings.findLockedById(listingId).isEmpty()) {
      throw notFound();
    }
    return images.findForListing(listingId).size();
  }

  @Override
  public UUID add(UUID listingId, String key) {
    ListingJpaEntity listing =
        listings.findById(listingId).orElseThrow(JpaListingImagesAdapter::notFound);
    int position = images.findForListing(listingId).size();
    ListingImageJpaEntity image = new ListingImageJpaEntity();
    image.setId(UUID.randomUUID());
    image.setListing(listing);
    image.setPosition(position);
    image.setImageReference(key);
    return images.saveAndFlush(image).getId();
  }

  @Override
  public String remove(UUID listingId, UUID imageId) {
    ListingImageJpaEntity targetImage =
        images
            .findForListingById(imageId, listingId)
            .orElseThrow(JpaListingImagesAdapter::notFound);
    String key = targetImage.getImageReference();
    int removedPosition = targetImage.getPosition();
    images.deleteForListingById(imageId, listingId);
    images.flush();
    List<ListingImageJpaEntity> remaining = images.findForListing(listingId);
    remaining.stream()
        .filter(image -> image.getPosition() > removedPosition)
        .forEach(image -> image.setPosition(image.getPosition() + 8));
    images.flush();
    remaining.stream()
        .filter(image -> image.getPosition() > removedPosition + 8)
        .forEach(image -> image.setPosition(image.getPosition() - 9));
    images.flush();
    return key;
  }

  @Override
  public void reorder(UUID listingId, List<UUID> imageIds) {
    List<ListingImageJpaEntity> current = images.findForListing(listingId);
    if (imageIds.size() != current.size()
        || !current.stream()
            .map(ListingImageJpaEntity::getId)
            .collect(java.util.stream.Collectors.toSet())
            .equals(java.util.Set.copyOf(imageIds))) {
      throw new IllegalArgumentException(
          "El orden debe contener todas las imágenes de la publicación.");
    }
    var byId =
        current.stream()
            .collect(
                java.util.stream.Collectors.toMap(ListingImageJpaEntity::getId, image -> image));
    current.forEach(image -> image.setPosition(image.getPosition() + 8));
    images.flush();
    for (int index = 0; index < imageIds.size(); index++) {
      byId.get(imageIds.get(index)).setPosition(index);
    }
    images.flush();
  }

  private static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("LISTING_IMAGE_NOT_FOUND", "No se encontró la imagen.");
  }
}
