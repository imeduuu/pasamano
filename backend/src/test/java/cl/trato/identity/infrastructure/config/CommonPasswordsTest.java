package cl.trato.identity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.PasswordPolicy;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** La lista de contraseñas comunes del recurso tiene al menos 1.000 entradas utilizables. */
class CommonPasswordsTest {

  @Test
  void the_resource_has_at_least_1000_entries_without_comments() {
    List<String> passwords =
        IdentityConfiguration.loadCommonPasswords(
            new ClassPathResource(IdentityConfiguration.COMMON_PASSWORDS));

    assertThat(passwords)
        .hasSizeGreaterThanOrEqualTo(1000)
        .noneMatch(p -> p.startsWith("#"))
        .noneMatch(String::isBlank)
        .contains("password123", "qwertyuiop", "contrasena123");
  }

  @Test
  void the_production_policy_rejects_listed_passwords() {
    PasswordPolicy policy = new IdentityConfiguration().passwordPolicy();

    assertThatThrownBy(() -> policy.validate("Password123"))
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_COMMON");
  }

  @Test
  void a_missing_resource_fails_fast() {
    assertThatThrownBy(
            () -> IdentityConfiguration.loadCommonPasswords(new ClassPathResource("no-existe.txt")))
        .isInstanceOf(UncheckedIOException.class);
  }
}
