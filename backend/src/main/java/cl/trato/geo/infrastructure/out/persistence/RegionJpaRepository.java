package cl.trato.geo.infrastructure.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegionJpaRepository extends JpaRepository<RegionJpaEntity, String> {
  List<RegionJpaEntity> findAllByOrderBySortOrderAsc();
}
