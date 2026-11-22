package cl.trato.identity.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Los eventos que llevan tokens en claro nunca los exponen al imprimirse (RNF-OBS-01). */
class IdentityEventsTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");
  static final Email EMAIL = new Email("ana.perez@ejemplo.cl");

  @Test
  void the_verification_request_hides_the_token_and_masks_the_email() {
    var event =
        new VerificationEmailRequested(UserId.newId(), EMAIL, "Ana", "token-secreto", NOW, NOW);

    assertThat(event.toString())
        .doesNotContain("token-secreto")
        .doesNotContain("ana.perez@")
        .contains("a***@ejemplo.cl");
    assertThat(event.eventType()).isEqualTo("VerificationEmailRequested");
  }

  @Test
  void the_password_reset_request_hides_the_token() {
    var event = new PasswordResetRequested(UserId.newId(), EMAIL, "Ana", "token-secreto", NOW, NOW);

    assertThat(event.toString()).doesNotContain("token-secreto").doesNotContain("ana.perez@");
  }

  @Test
  void the_other_events_carry_no_secret() {
    UserId id = UserId.newId();

    assertThat(new UserRegistered(id, EMAIL, NOW).occurredAt()).isEqualTo(NOW);
    assertThat(new RegistrationAttemptedWithExistingEmail(EMAIL, "Ana", NOW).toString())
        .doesNotContain("ana.perez@");
  }
}
