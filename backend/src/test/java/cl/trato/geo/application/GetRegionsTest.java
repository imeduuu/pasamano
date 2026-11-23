package cl.trato.geo.application;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.geo.domain.Commune;
import cl.trato.geo.domain.Region;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Consulta pública de regiones y comunas (RF-PUB-03). */
class GetRegionsTest {

  @Test
  void keeps_the_repository_region_order_and_sorts_communes_alphabetically_in_spanish() {
    Region arica =
        new Region(
            "15",
            "Arica y Parinacota",
            List.of(new Commune("15101", "Arica", "15"), new Commune("15102", "Camarones", "15")));
    Region nuble =
        new Region(
            "16",
            "Ñuble",
            List.of(
                new Commune("16305", "San Nicolás", "16"),
                new Commune("16104", "Ñiquén", "16"),
                new Commune("16101", "Chillán", "16"),
                new Commune("16302", "Ninhue", "16")));
    GetRegions query = new GetRegions(() -> List.of(arica, nuble));

    assertThat(query.execute())
        .containsExactly(
            new RegionView(
                "15",
                "Arica y Parinacota",
                List.of(new CommuneView("15101", "Arica"), new CommuneView("15102", "Camarones"))),
            new RegionView(
                "16",
                "Ñuble",
                List.of(
                    new CommuneView("16101", "Chillán"),
                    new CommuneView("16302", "Ninhue"),
                    new CommuneView("16104", "Ñiquén"),
                    new CommuneView("16305", "San Nicolás"))));
  }
}
