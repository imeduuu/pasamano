package cl.trato.geo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.geo.application.port.RegionRepository;
import cl.trato.geo.domain.Commune;
import cl.trato.geo.domain.Region;
import cl.trato.support.IntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de referencia cargados por la migración V10, contrastados con la fuente oficial: SUBDERE,
 * Códigos Únicos Territoriales actualizados a 2018 (archivo {@code CUT_2018_v04.xls}, Decreto
 * Exento N° 1.115 de 2018): 16 regiones y 346 comunas.
 */
@IntegrationTest
class RegionRepositoryIT {
  @Autowired RegionRepository regions;
  @Autowired JdbcTemplate jdbc;

  /** Comunas por región según la fuente oficial, en orden geográfico de norte a sur. */
  private static final Map<String, Integer> COMMUNES_PER_REGION = new LinkedHashMap<>();

  static {
    COMMUNES_PER_REGION.put("15", 4);
    COMMUNES_PER_REGION.put("01", 7);
    COMMUNES_PER_REGION.put("02", 9);
    COMMUNES_PER_REGION.put("03", 9);
    COMMUNES_PER_REGION.put("04", 15);
    COMMUNES_PER_REGION.put("05", 38);
    COMMUNES_PER_REGION.put("13", 52);
    COMMUNES_PER_REGION.put("06", 33);
    COMMUNES_PER_REGION.put("07", 30);
    COMMUNES_PER_REGION.put("16", 21);
    COMMUNES_PER_REGION.put("08", 33);
    COMMUNES_PER_REGION.put("09", 32);
    COMMUNES_PER_REGION.put("14", 12);
    COMMUNES_PER_REGION.put("10", 30);
    COMMUNES_PER_REGION.put("11", 10);
    COMMUNES_PER_REGION.put("12", 11);
  }

  @Test
  void loads_the_16_regions_and_346_communes_of_the_official_source() {
    List<Region> all = regions.findAll();

    assertThat(all).hasSize(16);
    assertThat(all.stream().mapToInt(region -> region.communes().size()).sum()).isEqualTo(346);
    assertThat(all)
        .extracting(Region::code)
        .containsExactlyElementsOf(COMMUNES_PER_REGION.keySet());
    all.forEach(region ->
        assertThat(region.communes())
            .as("comunas de la región %s", region.code())
            .hasSize(COMMUNES_PER_REGION.get(region.code())));
  }

  @Test
  void keeps_official_codes_and_names() {
    Map<String, Region> byCode = new LinkedHashMap<>();
    regions.findAll().forEach(region -> byCode.put(region.code(), region));

    assertThat(byCode.get("13").name()).isEqualTo("Metropolitana de Santiago");
    assertThat(byCode.get("13").requireCommune("13101"))
        .isEqualTo(new Commune("13101", "Santiago", "13"));
    assertThat(byCode.get("16").name()).isEqualTo("Ñuble");
    assertThat(byCode.get("16").requireCommune("16101").name()).isEqualTo("Chillán");
    assertThat(byCode.get("06").name()).isEqualTo("Libertador General Bernardo O'Higgins");
    assertThat(byCode.get("11").requireCommune("11101").name()).isEqualTo("Coyhaique");
  }

  @Test
  void database_rejects_a_commune_whose_code_does_not_match_its_region() {
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into communes(code, name, region_code) values (?, ?, ?)",
                "05999",
                "Comuna inventada",
                "13"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
