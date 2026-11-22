package cl.trato.catalog.infrastructure.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
  List<CategoryJpaEntity> findAllByActiveTrueOrderBySortOrderAscNameAsc();

  boolean existsBySlug(String slug);

  boolean existsByParentId(UUID parentId);
}
