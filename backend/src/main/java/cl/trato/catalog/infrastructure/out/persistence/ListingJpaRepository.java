package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.domain.ListingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ListingJpaRepository extends JpaRepository<ListingJpaEntity, UUID> {
  @Override
  @EntityGraph(attributePaths = "images")
  Optional<ListingJpaEntity> findById(UUID id);

  @EntityGraph(attributePaths = "images")
  List<ListingJpaEntity> findBySellerIdAndStatusNotOrderById(
      UUID sellerId,
      ListingStatus excludedStatus,
      org.springframework.data.domain.Pageable pageable);

  @EntityGraph(attributePaths = "images")
  List<ListingJpaEntity> findBySellerIdAndStatusOrderById(
      UUID sellerId, ListingStatus status, org.springframework.data.domain.Pageable pageable);

  long countBySellerIdAndStatus(UUID sellerId, ListingStatus status);

  long countBySellerId(UUID sellerId);

  long countBySellerIdAndStatusNot(UUID sellerId, ListingStatus status);
}
