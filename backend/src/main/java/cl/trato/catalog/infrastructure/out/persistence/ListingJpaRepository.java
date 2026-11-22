package cl.trato.catalog.infrastructure.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ListingJpaRepository extends JpaRepository<ListingJpaEntity, UUID> {
  @Override
  @EntityGraph(attributePaths = "images")
  Optional<ListingJpaEntity> findById(UUID id);
}
