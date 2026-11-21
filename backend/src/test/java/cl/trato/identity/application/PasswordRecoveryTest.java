package cl.trato.identity.application;

import static cl.trato.identity.application.IdentityFakes.GOOD_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.event.PasswordResetRequested;
import cl.trato.identity.domain.exception.InvalidCredentialsException;
import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** CU-16 Recuperar contraseña (RF-AUT-06). */
class PasswordRecoveryTest {

  static final String NEW_PASSWORD = "otra-clave-muy-segura";

  final IdentityFakes fakes = new IdentityFakes();

  @Test
  void an_existing_account_receives_a_link_valid_for_one_hour() {
    User user = fakes.activeUser("ana@ejemplo.cl");

    fakes.requestPasswordReset().execute("ANA@ejemplo.cl");

    assertThat(fakes.events.ofType(PasswordResetRequested.class))
        .singleElement()
        .satisfies(event -> {
          assertThat(event.userId()).isEqualTo(user.id());
          assertThat(event.token()).isEqualTo("token-1");
          assertThat(event.expiresAt()).isEqualTo(IdentityFakes.START.plus(Duration.ofHours(1)));
        });
    assertThat(fakes.oneTimeTokens.of(user.id(), TokenPurpose.PASSWORD_RESET))
        .singleElement()
        .satisfies(token -> assertThat(token.tokenHash()).isEqualTo("sha(token-1)"));
  }

  @Test
  void unknown_or_malformed_emails_get_the_same_silent_answer() {
    fakes.requestPasswordReset().execute("nadie@ejemplo.cl");
    fakes.requestPasswordReset().execute("no-es-correo");

    assertThat(fakes.events.published).isEmpty();
  }

  @Test
  void suspended_accounts_do_not_receive_a_link() {
    User user = fakes.activeUser("ana@ejemplo.cl");
    user.suspend(fakes.clock.instant());
    fakes.users.save(user);

    fakes.requestPasswordReset().execute("ana@ejemplo.cl");

    assertThat(fakes.events.published).isEmpty();
  }

  @Test
  void a_new_request_invalidates_the_previous_link() {
    fakes.activeUser("ana@ejemplo.cl");
    fakes.requestPasswordReset().execute("ana@ejemplo.cl");
    fakes.requestPasswordReset().execute("ana@ejemplo.cl");

    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("token-1", NEW_PASSWORD)))
        .extracting("code")
        .isEqualTo("TOKEN_ALREADY_USED");
    fakes.resetPassword().execute(new ResetPasswordCommand("token-2", NEW_PASSWORD));
  }

  @Test
  void resetting_changes_the_password_and_revokes_every_refresh_token() {
    User user = fakes.activeUser("ana@ejemplo.cl");
    AuthSession session = fakes.login().execute(new LoginCommand("ana@ejemplo.cl", GOOD_PASSWORD));
    fakes.requestPasswordReset().execute("ana@ejemplo.cl");
    String resetToken = fakes.tokenGenerator.last;

    fakes.resetPassword().execute(new ResetPasswordCommand(resetToken, NEW_PASSWORD));

    assertThat(fakes.refreshTokens.ofUser(user.id()))
        .allSatisfy(token -> assertThat(token.revokedAt()).isNotNull());
    assertThatThrownBy(() -> fakes.refreshSession().execute(session.refreshToken()))
        .isInstanceOf(RefreshTokenReuseException.class);
    assertThatThrownBy(
            () -> fakes.login().execute(new LoginCommand("ana@ejemplo.cl", GOOD_PASSWORD)))
        .isInstanceOf(InvalidCredentialsException.class);
    assertThat(fakes.login().execute(new LoginCommand("ana@ejemplo.cl", NEW_PASSWORD))).isNotNull();
  }

  @Test
  void the_link_is_single_use_and_expires_after_one_hour() {
    fakes.activeUser("ana@ejemplo.cl");
    fakes.requestPasswordReset().execute("ana@ejemplo.cl");
    fakes.resetPassword().execute(new ResetPasswordCommand("token-1", NEW_PASSWORD));

    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("token-1", NEW_PASSWORD)))
        .extracting("code")
        .isEqualTo("TOKEN_ALREADY_USED");

    fakes.requestPasswordReset().execute("ana@ejemplo.cl");
    fakes.clock.advance(Duration.ofHours(1));
    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("token-2", NEW_PASSWORD)))
        .extracting("code")
        .isEqualTo("TOKEN_EXPIRED");
  }

  @Test
  void the_new_password_must_follow_the_policy_and_the_token_must_exist() {
    fakes.activeUser("ana@ejemplo.cl");
    fakes.requestPasswordReset().execute("ana@ejemplo.cl");

    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("token-1", "qwertyuiop")))
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_COMMON");
    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("otro", NEW_PASSWORD)))
        .extracting("code")
        .isEqualTo("TOKEN_INVALID");
  }

  @Test
  void a_verification_token_cannot_be_used_to_reset_the_password() {
    fakes
        .registerUser()
        .execute(new RegisterUserCommand("Ana", "ana@ejemplo.cl", GOOD_PASSWORD, true));

    assertThatThrownBy(
            () -> fakes.resetPassword().execute(new ResetPasswordCommand("token-1", NEW_PASSWORD)))
        .extracting("code")
        .isEqualTo("TOKEN_INVALID");
  }
}
