package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

  @Test
  void normalizes_to_lowercase_without_surrounding_spaces() {
    assertThat(new Email("  Ana.Perez@Ejemplo.CL ").value()).isEqualTo("ana.perez@ejemplo.cl");
  }

  @Test
  void two_emails_differing_only_in_case_are_equal() {
    assertThat(new Email("ANA@ejemplo.cl")).isEqualTo(new Email("ana@ejemplo.cl"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "sin-arroba", "a@b", "a @b.cl", "@ejemplo.cl", "ana@", "a@@b.cl"})
  void rejects_invalid_formats(String raw) {
    assertThatThrownBy(() -> new Email(raw))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("INVALID_EMAIL");
  }

  @Test
  void rejects_addresses_longer_than_254_characters() {
    String tooLong = "a".repeat(245) + "@ejemplo.cl";

    assertThatThrownBy(() -> new Email(tooLong)).isInstanceOf(DomainValidationException.class);
  }

  @Test
  void masks_the_address_for_logs() {
    assertThat(new Email("ana.perez@ejemplo.cl").masked()).isEqualTo("a***@ejemplo.cl");
    assertThat(new Email("a@ejemplo.cl").masked()).isEqualTo("*@ejemplo.cl");
  }
}
