package cl.trato.catalog.infrastructure.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ListingImageJpaRepository extends JpaRepository<ListingImageJpaEntity, UUID> {
  @Query(
      "select image from ListingImageJpaEntity image where image.listing.id = :listingId order by image.position")
  List<ListingImageJpaEntity> findForListing(@Param("listingId") UUID listingId);

  @Query(
      "select image from ListingImageJpaEntity image where image.id = :imageId and image.listing.id = :listingId")
  Optional<ListingImageJpaEntity> findForListingById(
      @Param("imageId") UUID imageId, @Param("listingId") UUID listingId);

  @Modifying
  @Query(
      "delete from ListingImageJpaEntity image where image.id = :imageId and image.listing.id = :listingId")
  int deleteForListingById(@Param("imageId") UUID imageId, @Param("listingId") UUID listingId);
}
