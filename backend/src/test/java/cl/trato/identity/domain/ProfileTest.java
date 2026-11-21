package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileTest {

  @Nested
  class DisplayNames {

    @Test
    void trims_and_collapses_spaces() {
      assertThat(new DisplayName("  Ana   Pérez ").value()).isEqualTo("Ana Pérez");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A"})
    void must_have_at_least_two_characters(String raw) {
      assertThatThrownBy(() -> new DisplayName(raw))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("INVALID_NAME");
    }

    @Test
    void must_have_at_most_sixty_characters() {
      assertThat(new DisplayName("a".repeat(60)).value()).hasSize(60);
      assertThatThrownBy(() -> new DisplayName("a".repeat(61)))
          .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejects_control_characters() {
      assertThatThrownBy(() -> new DisplayName("Ana\u0000Pérez"))
          .isInstanceOf(DomainValidationException.class);
    }
  }

  @Nested
  class Profiles {

    @Test
    void a_new_profile_only_has_the_display_name() {
      Profile profile = Profile.withName(new DisplayName("Ana"));

      assertThat(profile.name().value()).isEqualTo("Ana");
      assertThat(profile.region()).isNull();
      assertThat(profile.commune()).isNull();
      assertThat(profile.photoUrl()).isNull();
    }

    @Test
    void blank_optional_fields_become_null() {
      Profile profile = new Profile(new DisplayName("Ana"), "  ", "", " ");

      assertThat(profile.region()).isNull();
      assertThat(profile.commune()).isNull();
      assertThat(profile.photoUrl()).isNull();
    }

    @Test
    void keeps_region_and_commune_trimmed() {
      Profile profile = new Profile(new DisplayName("Ana"), " Valparaíso ", " Viña del Mar ", null);

      assertThat(profile.region()).isEqualTo("Valparaíso");
      assertThat(profile.commune()).isEqualTo("Viña del Mar");
    }

    @Test
    void region_and_commune_have_at_most_sixty_characters() {
      assertThatThrownBy(() -> new Profile(new DisplayName("Ana"), "r".repeat(61), null, null))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("INVALID_REGION");
      assertThatThrownBy(() -> new Profile(new DisplayName("Ana"), null, "c".repeat(61), null))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("INVALID_COMMUNE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://img.ejemplo.cl/ana.webp", "http://localhost:9000/a.png"})
    void accepts_absolute_http_photo_urls(String url) {
      assertThat(new Profile(new DisplayName("Ana"), null, null, url).photoUrl()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"javascript:alert(1)", "ftp://x.cl/a.png", "/relativa.png", "no es url"})
    void rejects_non_http_photo_urls(String url) {
      assertThatThrownBy(() -> new Profile(new DisplayName("Ana"), null, null, url))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("INVALID_PHOTO_URL");
    }

    @Test
    void rejects_photo_urls_longer_than_500_characters() {
      String url = "https://img.ejemplo.cl/" + "a".repeat(480);

      assertThatThrownBy(() -> new Profile(new DisplayName("Ana"), null, null, url))
          .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void the_display_name_is_required() {
      assertThatThrownBy(() -> new Profile(null, null, null, null))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("INVALID_NAME");
    }
  }
}
