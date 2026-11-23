package cl.trato.geo.application;

import cl.trato.geo.application.port.RegionRepository;
import cl.trato.geo.domain.Commune;
import cl.trato.geo.domain.Region;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Obtiene las regiones de Chile con sus comunas para elegir región y comuna (RF-PUB-03). */
public class GetRegions {
  private static final Locale SPANISH_CHILE = Locale.forLanguageTag("es-CL");

  private final RegionRepository regions;

  public GetRegions(RegionRepository regions) {
    this.regions = regions;
  }

  public List<RegionView> execute() {
    Comparator<Commune> byName =
        Comparator.comparing(Commune::name, Collator.getInstance(SPANISH_CHILE));
    return regions.findAll().stream().map(region -> view(region, byName)).toList();
  }

  private static RegionView view(Region region, Comparator<Commune> byName) {
    List<CommuneView> communes =
        region.communes().stream()
            .sorted(byName)
            .map(commune -> new CommuneView(commune.code(), commune.name()))
            .toList();
    return new RegionView(region.code(), region.name(), communes);
  }
}
