package cl.trato.geo.infrastructure.config;

import cl.trato.geo.application.GetRegions;
import cl.trato.geo.application.port.RegionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado de los casos de uso de regiones y comunas. */
@Configuration
public class GeoConfiguration {
  @Bean
  GetRegions getRegions(RegionRepository regions) {
    return new GetRegions(regions);
  }
}
