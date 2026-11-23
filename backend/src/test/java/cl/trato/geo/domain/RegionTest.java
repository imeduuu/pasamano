package cl.trato.geo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regla "la comuna pertenece a la región" sobre los Códigos Únicos Territoriales (RF-PUB-03). */
class RegionTest {

  private static final Commune SANTIAGO = new Commune("13101", "Santiago", "13");
  private static final Commune PUENTE_ALTO = new Commune("13201", "Puente Alto", "13");
  private static final Commune VALPARAISO = new Commune("05101", "Valparaíso", "05");
  private static final Region METROPOLITANA =
      new Region("13", "Metropolitana de Santiago", List.of(SANTIAGO, PUENTE_ALTO));

  @Test
  void commune_belongs_to_the_region_of_its_code() {
    assertThat(SANTIAGO.belongsTo(METROPOLITANA)).isTrue();
    assertThat(VALPARAISO.belongsTo(METROPOLITANA)).isFalse();
  }

  @Test
  void region_finds_its_own_commune_by_code() {
    assertThat(METROPOLITANA.contains("13201")).isTrue();
    assertThat(METROPOLITANA.contains("05101")).isFalse();
    assertThat(METROPOLITANA.requireCommune("13101")).isEqualTo(SANTIAGO);
  }

  @Test
  void rejects_a_commune_that_is_not_part_of_the_region() {
    assertThatThrownBy(() -> METROPOLITANA.requireCommune("05101"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("no pertenece a la región")
        .extracting("code")
        .isEqualTo("COMMUNE_NOT_IN_REGION");
  }

  @Test
  void region_cannot_be_built_with_communes_of_another_region() {
    assertThatThrownBy(
            () -> new Region("13", "Metropolitana de Santiago", List.of(SANTIAGO, VALPARAISO)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("COMMUNE_NOT_IN_REGION");
  }

  @Test
  void commune_code_must_start_with_its_region_code() {
    assertThatThrownBy(() -> new Commune("05101", "Valparaíso", "13"))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("INVALID_COMMUNE_CODE");
  }

  @Test
  void rejects_codes_that_do_not_follow_the_official_format_and_blank_names() {
    assertThatThrownBy(() -> new Region("1", "Tarapacá", List.of()))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("INVALID_REGION_CODE");
    assertThatThrownBy(() -> new Region("01", " ", List.of()))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> new Commune("1310", "Santiago", "13"))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("INVALID_COMMUNE_CODE");
    assertThatThrownBy(() -> new Commune("13101", "", "13"))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> new Commune("13101", "Santiago", null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void communes_of_a_region_are_immutable() {
    assertThatThrownBy(() -> METROPOLITANA.communes().add(VALPARAISO))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
