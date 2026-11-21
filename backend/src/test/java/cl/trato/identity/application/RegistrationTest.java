package cl.trato.identity.application;

import static cl.trato.identity.application.IdentityFakes.GOOD_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.event.RegistrationAttemptedWithExistingEmail;
import cl.trato.identity.domain.event.UserRegistered;
import cl.trato.identity.domain.event.VerificationEmailRequested;
import cl.trato.identity.domain.exception.InvalidTokenException;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** CU-01 Registrarse (RF-AUT-01, RF-AUT-02, RNF-LEG-01). */
class RegistrationTest {

  final IdentityFakes fakes = new IdentityFakes();

  RegisterUserCommand command(String email) {
    return new RegisterUserCommand("Ana Pérez", email, GOOD_PASSWORD, true);
  }

  @Nested
  class RegisterUserUseCase {

    @Test
    void creates_a_pending_account_with_hashed_password_and_consent() {
      fakes.registerUser().execute(command("Ana@Ejemplo.cl"));

      User user = fakes.users.findByEmail(new Email("ana@ejemplo.cl")).orElseThrow();
      assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
      assertThat(user.passwordHash().value()).isEqualTo("hashed:" + GOOD_PASSWORD);
      assertThat(fakes.consents.saved)
          .singleElement()
          .satisfies(consent -> {
            assertThat(consent.userId()).isEqualTo(user.id());
            assertThat(consent.termsVersion()).isEqualTo("2026-11-20");
            assertThat(consent.privacyVersion()).isEqualTo("2026-11-20");
            assertThat(consent.acceptedAt()).isEqualTo(IdentityFakes.START);
          });
    }

    @Test
    void issues_a_hashed_single_use_verification_token_valid_for_24_hours() {
      fakes.registerUser().execute(command("ana@ejemplo.cl"));

      User user = fakes.users.findByEmail(new Email("ana@ejemplo.cl")).orElseThrow();
      assertThat(fakes.oneTimeTokens.of(user.id(), TokenPurpose.EMAIL_VERIFICATION))
          .singleElement()
          .satisfies(token -> {
            assertThat(token.tokenHash()).isEqualTo("sha(token-1)");
            assertThat(token.expiresAt()).isEqualTo(IdentityFakes.START.plus(Duration.ofHours(24)));
          });
    }

    @Test
    void publishes_the_registration_and_the_verification_email_request() {
      fakes.registerUser().execute(command("ana@ejemplo.cl"));

      assertThat(fakes.events.ofType(UserRegistered.class)).hasSize(1);
      assertThat(fakes.events.ofType(VerificationEmailRequested.class))
          .singleElement()
          .satisfies(event -> {
            assertThat(event.email().value()).isEqualTo("ana@ejemplo.cl");
            assertThat(event.name()).isEqualTo("Ana Pérez");
            assertThat(event.token()).isEqualTo("token-1");
          });
    }

    @Test
    void an_existing_email_creates_nothing_and_notifies_the_owner_instead() {
      fakes.activeUser("ana@ejemplo.cl");
      int usersBefore = fakes.users.byId.size();

      fakes.registerUser().execute(command("ANA@ejemplo.cl"));

      assertThat(fakes.users.byId).hasSize(usersBefore);
      assertThat(fakes.consents.saved).isEmpty();
      assertThat(fakes.events.ofType(VerificationEmailRequested.class)).isEmpty();
      assertThat(fakes.events.ofType(RegistrationAttemptedWithExistingEmail.class))
          .singleElement()
          .satisfies(event -> assertThat(event.email().value()).isEqualTo("ana@ejemplo.cl"));
    }

    @Test
    void terms_and_privacy_must_be_accepted() {
      assertThatThrownBy(() ->
              fakes
                  .registerUser()
                  .execute(new RegisterUserCommand("Ana", "ana@ejemplo.cl", GOOD_PASSWORD, false)))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("TERMS_NOT_ACCEPTED");
      assertThat(fakes.users.byId).isEmpty();
    }

    @Test
    void the_password_policy_is_applied_before_checking_whether_the_email_exists() {
      fakes.activeUser("ana@ejemplo.cl");

      assertThatThrownBy(() ->
              fakes
                  .registerUser()
                  .execute(new RegisterUserCommand("Ana", "ana@ejemplo.cl", "corta", true)))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("PASSWORD_TOO_SHORT");
      assertThat(fakes.events.published).isEmpty();
    }

    @Test
    void rejects_common_passwords_and_invalid_data() {
      assertThatThrownBy(() ->
              fakes
                  .registerUser()
                  .execute(new RegisterUserCommand("Ana", "ana@ejemplo.cl", "Password123", true)))
          .extracting("code")
          .isEqualTo("PASSWORD_TOO_COMMON");
      assertThatThrownBy(() ->
              fakes
                  .registerUser()
                  .execute(new RegisterUserCommand("Ana", "no-es-correo", GOOD_PASSWORD, true)))
          .extracting("code")
          .isEqualTo("INVALID_EMAIL");
      assertThatThrownBy(() ->
              fakes
                  .registerUser()
                  .execute(new RegisterUserCommand("A", "ana@ejemplo.cl", GOOD_PASSWORD, true)))
          .extracting("code")
          .isEqualTo("INVALID_NAME");
    }
  }

  @Nested
  class VerifyEmailUseCase {

    @Test
    void a_valid_link_activates_the_account_and_cannot_be_used_twice() {
      fakes.registerUser().execute(command("ana@ejemplo.cl"));

      fakes.verifyEmail().execute("token-1");

      User user = fakes.users.findByEmail(new Email("ana@ejemplo.cl")).orElseThrow();
      assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
      assertThatThrownBy(() -> fakes.verifyEmail().execute("token-1"))
          .isInstanceOf(InvalidTokenException.class)
          .extracting("code")
          .isEqualTo("TOKEN_ALREADY_USED");
    }

    @Test
    void an_expired_link_does_not_activate_the_account() {
      fakes.registerUser().execute(command("ana@ejemplo.cl"));
      fakes.clock.advance(Duration.ofHours(24));

      assertThatThrownBy(() -> fakes.verifyEmail().execute("token-1"))
          .extracting("code")
          .isEqualTo("TOKEN_EXPIRED");
      assertThat(fakes.users.findByEmail(new Email("ana@ejemplo.cl")).orElseThrow().status())
          .isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void an_unknown_or_blank_link_is_rejected() {
      assertThatThrownBy(() -> fakes.verifyEmail().execute("inventado"))
          .extracting("code")
          .isEqualTo("TOKEN_INVALID");
      assertThatThrownBy(() -> fakes.verifyEmail().execute(" "))
          .extracting("code")
          .isEqualTo("TOKEN_INVALID");
    }

    @Test
    void a_password_reset_token_cannot_be_used_to_verify_the_email() {
      User user = fakes.pendingUser("ana@ejemplo.cl");
      fakes.tokenIssuer.issue(user.id(), TokenPurpose.PASSWORD_RESET, fakes.clock.instant());

      assertThatThrownBy(() -> fakes.verifyEmail().execute(fakes.tokenGenerator.last))
          .extracting("code")
          .isEqualTo("TOKEN_INVALID");
    }
  }

  @Nested
  class ResendVerificationUseCase {

    @Test
    void a_pending_account_gets_a_new_link_and_previous_links_stop_working() {
      fakes.registerUser().execute(command("ana@ejemplo.cl"));

      fakes.resendVerification().execute("ana@ejemplo.cl");

      assertThat(fakes.events.ofType(VerificationEmailRequested.class))
          .extracting(VerificationEmailRequested::token)
          .containsExactly("token-1", "token-2");
      assertThatThrownBy(() -> fakes.verifyEmail().execute("token-1"))
          .extracting("code")
          .isEqualTo("TOKEN_ALREADY_USED");
      fakes.verifyEmail().execute("token-2");
    }

    @Test
    void active_unknown_or_malformed_emails_get_no_email_and_no_error() {
      fakes.activeUser("activa@ejemplo.cl");

      fakes.resendVerification().execute("activa@ejemplo.cl");
      fakes.resendVerification().execute("nadie@ejemplo.cl");
      fakes.resendVerification().execute("no-es-correo");

      assertThat(fakes.events.published).isEmpty();
    }
  }
}
