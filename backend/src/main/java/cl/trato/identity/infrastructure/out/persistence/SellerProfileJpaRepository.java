package cl.trato.identity.infrastructure.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SellerProfileJpaRepository extends JpaRepository<SellerProfileJpaEntity, UUID> {

  Optional<SellerProfileJpaEntity> findByUserId(UUID userId);

  boolean existsByNormalizedStoreName(String normalizedStoreName);
}
