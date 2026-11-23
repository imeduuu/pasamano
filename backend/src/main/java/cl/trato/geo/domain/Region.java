package cl.trato.geo.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Región de Chile identificada por su Código Único Territorial (CUT) de dos dígitos, con sus
 * comunas. Garantiza la regla "la comuna pertenece a la región" usada en la región y comuna de
 * origen de una publicación (RF-PUB-03) y en las direcciones de despacho.
 */
public record Region(String code, String name, List<Commune> communes) {

  private static final Pattern CODE = Pattern.compile("\\d{2}");

  public Region {
    requireRegionCode(code);
    if (name == null || name.isBlank()) {
      throw new DomainValidationException("INVALID_REGION_NAME", "La región debe tener nombre");
    }
    communes = List.copyOf(communes);
    for (Commune commune : communes) {
      if (!commune.regionCode().equals(code)) {
        throw notInRegion(commune.code(), name);
      }
    }
  }

  public boolean contains(String communeCode) {
    return communes.stream().anyMatch(commune -> commune.code().equals(communeCode));
  }

  /** Devuelve la comuna indicada o falla si no pertenece a esta región. */
  public Commune requireCommune(String communeCode) {
    return communes.stream()
        .filter(commune -> commune.code().equals(communeCode))
        .findFirst()
        .orElseThrow(() -> notInRegion(communeCode, name));
  }

  static void requireRegionCode(String code) {
    if (code == null || !CODE.matcher(code).matches()) {
      throw new DomainValidationException(
          "INVALID_REGION_CODE", "El código de región debe tener 2 dígitos");
    }
  }

  private static DomainValidationException notInRegion(String communeCode, String regionName) {
    return new DomainValidationException(
        "COMMUNE_NOT_IN_REGION",
        "La comuna " + communeCode + " no pertenece a la región " + regionName);
  }
}
