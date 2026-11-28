package cl.trato.identity.infrastructure.out.geo;

import cl.trato.geo.application.port.RegionRepository;
import cl.trato.identity.application.port.ProfileLocationValidator;
import cl.trato.shared.domain.exception.DomainValidationException;
import org.springframework.stereotype.Component;

/** Adaptador entre identidad y el catálogo público de geo. */
@Component
class GeoProfileLocationValidator implements ProfileLocationValidator {

  private final RegionRepository regions;

  GeoProfileLocationValidator(RegionRepository regions) {
    this.regions = regions;
  }

  @Override
  public boolean isValid(String regionCode, String communeCode) {
    return regions.findAll().stream()
        .filter(region -> region.code().equals(regionCode))
        .findFirst()
        .map(region -> {
          try {
            region.requireCommune(communeCode);
            return true;
          } catch (DomainValidationException exception) {
            return false;
          }
        })
        .orElse(false);
  }
}
