package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Política de contraseña de CU-01 paso 3: mínimo 10 caracteres y no ser una contraseña común. */
class PasswordPolicyTest {

  private final PasswordPolicy policy =
      new PasswordPolicy(Set.of("password123", "qwertyuiop", "1234567890"));

  @Test
  void accepts_a_password_of_ten_or_more_characters_not_in_the_common_list() {
    assertThatCode(() -> policy.validate("caballo-bateria-grapa")).doesNotThrowAnyException();
    assertThatCode(() -> policy.validate("abcdefghi1")).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"corta", "123456789", "nueve9999"})
  void rejects_passwords_shorter_than_ten_characters(String raw) {
    assertThatThrownBy(() -> policy.validate(raw))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("10 caracteres")
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_SHORT");
  }

  @Test
  void counts_characters_not_bytes_for_the_minimum() {
    assertThatCode(() -> policy.validate("ñandú-ñandú")).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"password123", "PASSWORD123", "QwertyUiop", "1234567890"})
  void rejects_common_passwords_ignoring_case(String raw) {
    assertThatThrownBy(() -> policy.validate(raw))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_COMMON");
  }

  @Test
  void rejects_passwords_longer_than_72_bytes_because_bcrypt_would_truncate_them() {
    assertThatCode(() -> policy.validate("a".repeat(72))).doesNotThrowAnyException();
    assertThatThrownBy(() -> policy.validate("a".repeat(73)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_LONG");
    // 37 caracteres de 2 bytes = 74 bytes
    assertThatThrownBy(() -> policy.validate("ñ".repeat(37)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_LONG");
  }

  @Test
  void the_common_list_is_normalized_when_built() {
    PasswordPolicy mixedCase = new PasswordPolicy(Set.of("  MiClaveSecreta1 "));

    assertThatThrownBy(() -> mixedCase.validate("miclavesecreta1"))
        .isInstanceOf(DomainValidationException.class);
  }
}
