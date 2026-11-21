package cl.trato.identity.application;

import static cl.trato.identity.application.IdentityFakes.GOOD_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.AccountLockedException;
import cl.trato.identity.domain.exception.AccountSuspendedException;
import cl.trato.identity.domain.exception.EmailNotVerifiedException;
import cl.trato.identity.domain.exception.InvalidCredentialsException;
import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import cl.trato.identity.domain.exception.SessionExpiredException;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** CU-02 Iniciar sesión, renovación y cierre de sesión (RF-AUT-03 a RF-AUT-05, RF-AUT-08). */
class SessionTest {

  final IdentityFakes fakes = new IdentityFakes();

  AuthSession login(String email, String password) {
    return fakes.login().execute(new LoginCommand(email, password));
  }

  @Nested
  class LoginUseCase {

    @Test
    void issues_an_access_token_and_a_hashed_seven_day_refresh_token() {
      User user = fakes.activeUser("ana@ejemplo.cl");

      AuthSession session = login("ANA@ejemplo.cl", GOOD_PASSWORD);

      assertThat(session.accessToken()).isEqualTo("jwt-for-" + user.id());
      assertThat(session.accessTokenExpiresAt())
          .isEqualTo(IdentityFakes.START.plus(Duration.ofMinutes(15)));
      assertThat(session.refreshToken()).isEqualTo("token-1");
      assertThat(session.refreshTokenExpiresAt())
          .isEqualTo(IdentityFakes.START.plus(Duration.ofDays(7)));
      assertThat(session.userId()).isEqualTo(user.id());
      assertThat(fakes.refreshTokens.findByHash("sha(token-1)")).isPresent();
      assertThat(fakes.refreshTokens.findByHash("token-1")).isEmpty();
    }

    @Test
    void wrong_password_gives_a_generic_error_and_counts_the_attempt() {
      User user = fakes.activeUser("ana@ejemplo.cl");

      assertThatThrownBy(() -> login("ana@ejemplo.cl", "otra-clave-cualquiera"))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Correo o contraseña incorrectos.");
      assertThat(fakes.users.findById(user.id()).orElseThrow().failedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void unknown_email_gives_the_same_generic_error_and_spends_the_same_hashing_time() {
      assertThatThrownBy(() -> login("nadie@ejemplo.cl", GOOD_PASSWORD))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Correo o contraseña incorrectos.");
      assertThat(fakes.hasher.dummyChecks).isEqualTo(1);
    }

    @Test
    void a_malformed_email_is_treated_as_invalid_credentials() {
      assertThatThrownBy(() -> login("no-es-correo", GOOD_PASSWORD))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void the_fifth_consecutive_failure_locks_login_for_15_minutes_even_with_the_right_password() {
      fakes.activeUser("ana@ejemplo.cl");
      for (int i = 0; i < 4; i++) {
        assertThatThrownBy(() -> login("ana@ejemplo.cl", "clave-equivocada"))
            .isInstanceOf(InvalidCredentialsException.class);
      }

      assertThatThrownBy(() -> login("ana@ejemplo.cl", "clave-equivocada"))
          .isInstanceOf(AccountLockedException.class)
          .satisfies(ex ->
              assertThat(((AccountLockedException) ex).lockedUntil())
                  .isEqualTo(IdentityFakes.START.plus(Duration.ofMinutes(15))));
      assertThatThrownBy(() -> login("ana@ejemplo.cl", GOOD_PASSWORD))
          .isInstanceOf(AccountLockedException.class);

      fakes.clock.advance(Duration.ofMinutes(15));
      assertThat(login("ana@ejemplo.cl", GOOD_PASSWORD).accessToken()).isNotBlank();
    }

    @Test
    void a_successful_login_resets_the_failed_attempts() {
      User user = fakes.activeUser("ana@ejemplo.cl");
      assertThatThrownBy(() -> login("ana@ejemplo.cl", "clave-equivocada"));

      login("ana@ejemplo.cl", GOOD_PASSWORD);

      assertThat(fakes.users.findById(user.id()).orElseThrow().failedLoginAttempts()).isZero();
    }

    @Test
    void an_unverified_account_cannot_sign_in() {
      fakes.pendingUser("pendiente@ejemplo.cl");

      assertThatThrownBy(() -> login("pendiente@ejemplo.cl", GOOD_PASSWORD))
          .isInstanceOf(EmailNotVerifiedException.class);
      assertThat(fakes.refreshTokens.byId).isEmpty();
    }

    @Test
    void a_suspended_account_cannot_sign_in() {
      User user = fakes.activeUser("suspendida@ejemplo.cl");
      user.suspend(fakes.clock.instant());
      fakes.users.save(user);

      assertThatThrownBy(() -> login("suspendida@ejemplo.cl", GOOD_PASSWORD))
          .isInstanceOf(AccountSuspendedException.class);
      assertThat(fakes.refreshTokens.byId).isEmpty();
    }
  }

  @Nested
  class RefreshSessionUseCase {

    @Test
    void rotates_the_refresh_token_within_the_same_family() {
      fakes.activeUser("ana@ejemplo.cl");
      AuthSession first = login("ana@ejemplo.cl", GOOD_PASSWORD);
      fakes.clock.advance(Duration.ofMinutes(20));

      AuthSession second = fakes.refreshSession().execute(first.refreshToken());

      assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
      assertThat(second.accessTokenExpiresAt())
          .isEqualTo(fakes.clock.instant().plus(Duration.ofMinutes(15)));
      RefreshToken old = fakes.refreshTokens.findByHash("sha(" + first.refreshToken() + ")").get();
      RefreshToken next =
          fakes.refreshTokens.findByHash("sha(" + second.refreshToken() + ")").get();
      assertThat(old.usedAt()).isEqualTo(fakes.clock.instant());
      assertThat(next.familyId()).isEqualTo(old.familyId());
      assertThat(next.isActive(fakes.clock.instant())).isTrue();
    }

    @Test
    void reusing_an_already_rotated_token_revokes_the_whole_family() {
      fakes.activeUser("ana@ejemplo.cl");
      AuthSession first = login("ana@ejemplo.cl", GOOD_PASSWORD);
      AuthSession second = fakes.refreshSession().execute(first.refreshToken());

      assertThatThrownBy(() -> fakes.refreshSession().execute(first.refreshToken()))
          .isInstanceOf(RefreshTokenReuseException.class);

      RefreshToken current =
          fakes.refreshTokens.findByHash("sha(" + second.refreshToken() + ")").get();
      assertThat(fakes.refreshTokens.family(current.familyId()))
          .allSatisfy(token -> assertThat(token.revokedAt()).isNotNull());
      assertThatThrownBy(() -> fakes.refreshSession().execute(second.refreshToken()))
          .isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    void reuse_only_revokes_the_affected_family() {
      fakes.activeUser("ana@ejemplo.cl");
      AuthSession laptop = login("ana@ejemplo.cl", GOOD_PASSWORD);
      AuthSession phone = login("ana@ejemplo.cl", GOOD_PASSWORD);
      fakes.refreshSession().execute(laptop.refreshToken());

      assertThatThrownBy(() -> fakes.refreshSession().execute(laptop.refreshToken()))
          .isInstanceOf(RefreshTokenReuseException.class);

      assertThat(fakes.refreshSession().execute(phone.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void unknown_missing_or_expired_tokens_end_the_session() {
      fakes.activeUser("ana@ejemplo.cl");
      AuthSession session = login("ana@ejemplo.cl", GOOD_PASSWORD);

      assertThatThrownBy(() -> fakes.refreshSession().execute("inventado"))
          .isInstanceOf(SessionExpiredException.class);
      assertThatThrownBy(() -> fakes.refreshSession().execute(null))
          .isInstanceOf(SessionExpiredException.class);

      fakes.clock.advance(Duration.ofDays(7));
      assertThatThrownBy(() -> fakes.refreshSession().execute(session.refreshToken()))
          .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void a_user_suspended_after_signing_in_cannot_refresh() {
      User user = fakes.activeUser("ana@ejemplo.cl");
      AuthSession session = login("ana@ejemplo.cl", GOOD_PASSWORD);
      user.suspend(fakes.clock.instant());
      fakes.users.save(user);

      assertThatThrownBy(() -> fakes.refreshSession().execute(session.refreshToken()))
          .isInstanceOf(SessionExpiredException.class);
      assertThat(fakes.refreshTokens.ofUser(user.id()))
          .allSatisfy(token -> assertThat(token.revokedAt()).isNotNull());
    }
  }

  @Nested
  class LogoutUseCase {

    @Test
    void revokes_the_current_refresh_token() {
      User user = fakes.activeUser("ana@ejemplo.cl");
      AuthSession session = login("ana@ejemplo.cl", GOOD_PASSWORD);

      fakes.logout().execute(user.id(), session.refreshToken());

      assertThat(fakes.refreshTokens.ofUser(user.id()))
          .allSatisfy(token -> assertThat(token.revokedAt()).isNotNull());
      assertThatThrownBy(() -> fakes.refreshSession().execute(session.refreshToken()))
          .isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    void cannot_revoke_a_refresh_token_of_another_user() {
      fakes.activeUser("ana@ejemplo.cl");
      User other = fakes.activeUser("beto@ejemplo.cl");
      AuthSession anaSession = login("ana@ejemplo.cl", GOOD_PASSWORD);

      fakes.logout().execute(other.id(), anaSession.refreshToken());

      assertThat(fakes.refreshSession().execute(anaSession.refreshToken()).accessToken())
          .isNotBlank();
    }

    @Test
    void without_a_refresh_token_logout_does_nothing() {
      User user = fakes.activeUser("ana@ejemplo.cl");

      fakes.logout().execute(user.id(), null);
      fakes.logout().execute(user.id(), "inventado");

      assertThat(fakes.refreshTokens.byId).isEmpty();
    }
  }
}
