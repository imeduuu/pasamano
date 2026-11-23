package cl.trato.geo.infrastructure.out.persistence;

import cl.trato.geo.application.port.RegionRepository;
import cl.trato.geo.domain.Commune;
import cl.trato.geo.domain.Region;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adaptador JPA de lectura de regiones y comunas. */
@Component
class JpaRegionRepositoryAdapter implements RegionRepository {
  private final RegionJpaRepository regions;
  private final CommuneJpaRepository communes;

  JpaRegionRepositoryAdapter(RegionJpaRepository regions, CommuneJpaRepository communes) {
    this.regions = regions;
    this.communes = communes;
  }

  @Override
  public List<Region> findAll() {
    Map<String, List<Commune>> communesByRegion =
        communes.findAll().stream()
            .map(row -> new Commune(row.getCode(), row.getName(), row.getRegionCode()))
            .collect(Collectors.groupingBy(Commune::regionCode));
    return regions.findAllByOrderBySortOrderAsc().stream()
        .map(row ->
            new Region(
                row.getCode(),
                row.getName(),
                communesByRegion.getOrDefault(row.getCode(), List.of())))
        .toList();
  }
}
