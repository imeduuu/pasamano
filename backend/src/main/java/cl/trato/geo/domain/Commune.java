package cl.trato.geo.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.regex.Pattern;

/**
 * Comuna de Chile identificada por su Código Único Territorial (CUT) de cinco dígitos. Los dos
 * primeros dígitos del CUT son el código de su región (Decreto Exento N° 1.115 de 2018).
 */
public record Commune(String code, String name, String regionCode) {

  private static final Pattern CODE = Pattern.compile("\\d{5}");

  public Commune {
    Region.requireRegionCode(regionCode);
    if (code == null || !CODE.matcher(code).matches() || !code.startsWith(regionCode)) {
      throw new DomainValidationException(
          "INVALID_COMMUNE_CODE",
          "El código de comuna debe tener 5 dígitos y comenzar con el código de su región");
    }
    if (name == null || name.isBlank()) {
      throw new DomainValidationException("INVALID_COMMUNE_NAME", "La comuna debe tener nombre");
    }
  }

  /** La comuna pertenece a la región cuando comparten el código regional del CUT. */
  public boolean belongsTo(Region region) {
    return regionCode.equals(region.code());
  }
}
