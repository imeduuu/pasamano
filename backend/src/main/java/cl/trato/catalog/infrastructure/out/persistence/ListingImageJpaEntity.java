package cl.trato.catalog.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Imagen ordenada perteneciente a una publicación. */
@Entity
@Table(name = "listing_images")
public class ListingImageJpaEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listing_id", nullable = false)
  private ListingJpaEntity listing;

  @Column(name = "position", nullable = false)
  private int position;

  @Column(name = "image_reference", nullable = false, length = 500)
  private String imageReference;

  @Column(name = "thumbnail_reference", nullable = false, length = 500)
  private String thumbnailReference;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ListingJpaEntity getListing() {
    return listing;
  }

  public void setListing(ListingJpaEntity listing) {
    this.listing = listing;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public String getImageReference() {
    return imageReference;
  }

  public void setImageReference(String imageReference) {
    this.imageReference = imageReference;
  }

  public String getThumbnailReference() {
    return thumbnailReference;
  }

  public void setThumbnailReference(String thumbnailReference) {
    this.thumbnailReference = thumbnailReference;
  }
}
