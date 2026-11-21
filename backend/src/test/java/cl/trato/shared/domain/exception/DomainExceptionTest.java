package cl.trato.shared.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

  @Test
  void validation_exception_carries_code_message_and_category() {
    DomainException ex = new DomainValidationException("TITLE_TOO_SHORT", "El título es corto.");

    assertThat(ex.code()).isEqualTo("TITLE_TOO_SHORT");
    assertThat(ex.getMessage()).isEqualTo("El título es corto.");
    assertThat(ex.category()).isEqualTo(ErrorCategory.INVALID_INPUT);
  }

  @Test
  void business_rule_violation_has_its_own_category() {
    DomainException ex =
        new BusinessRuleViolationException("OWN_LISTING", "No puedes comprar tu publicación.");

    assertThat(ex.category()).isEqualTo(ErrorCategory.BUSINESS_RULE);
  }

  @Test
  void not_found_has_its_own_category() {
    DomainException ex = new ResourceNotFoundException("USER_NOT_FOUND", "No existe el usuario.");

    assertThat(ex.category()).isEqualTo(ErrorCategory.NOT_FOUND);
  }

  @Test
  void conflict_has_its_own_category() {
    DomainException ex = new ConflictException("STALE_VERSION", "El recurso cambió.");

    assertThat(ex.category()).isEqualTo(ErrorCategory.CONFLICT);
  }

  @Test
  void subclasses_can_declare_any_category() {
    DomainException ex =
        new DomainException("ACCOUNT_LOCKED", ErrorCategory.LOCKED, "Cuenta bloqueada.") {};

    assertThat(ex.category()).isEqualTo(ErrorCategory.LOCKED);
  }

  @Test
  void code_must_be_upper_snake_case_to_remain_stable() {
    assertThatThrownBy(() -> new DomainValidationException("bad code", "x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DomainValidationException(null, "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void category_is_required() {
    assertThatThrownBy(() -> new DomainException("SOME_CODE", null, "x") {})
        .isInstanceOf(NullPointerException.class);
  }
}
