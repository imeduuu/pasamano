package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.event.UserRegistered;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");

  static User newUser() {
    return User.register(
        UserId.newId(),
        new Email("ana@ejemplo.cl"),
        new PasswordHash("$2a$12$hash"),
        new DisplayName("Ana Pérez"),
        NOW);
  }

  @Nested
  class Registration {

    @Test
    void a_new_user_starts_pending_verification_with_the_user_role() {
      User user = newUser();

      assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
      assertThat(user.roles()).containsExactly(Role.USER);
      assertThat(user.failedLoginAttempts()).isZero();
      assertThat(user.createdAt()).isEqualTo(NOW);
      assertThat(user.updatedAt()).isEqualTo(NOW);
      assertThat(user.profile().name().value()).isEqualTo("Ana Pérez");
      assertThat(user.version()).isNull();
    }

    @Test
    void registration_raises_the_user_registered_event() {
      User user = newUser();

      assertThat(user.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              UserRegistered.class,
              event -> {
                assertThat(event.userId()).isEqualTo(user.id());
                assertThat(event.email()).isEqualTo(user.email());
                assertThat(event.occurredAt()).isEqualTo(NOW);
              });
    }

    @Test
    void roles_cannot_be_modified_from_outside() {
      User user = newUser();

      assertThatThrownBy(() -> user.roles().add(Role.ADMIN))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class EmailVerification {

    @Test
    void verifying_the_email_activates_the_account() {
      User user = newUser();

      user.verifyEmail(NOW.plusSeconds(60));

      assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void verifying_an_active_account_again_keeps_it_active() {
      User user = newUser();
      user.verifyEmail(NOW);

      user.verifyEmail(NOW.plusSeconds(5));

      assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void verifying_does_not_reactivate_a_suspended_account() {
      User user = newUser();
      user.verifyEmail(NOW);
      user.suspend(NOW);

      user.verifyEmail(NOW.plusSeconds(5));

      assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
    }
  }

  @Nested
  class LoginLockout {

    @Test
    void four_failed_attempts_do_not_lock_the_account() {
      User user = newUser();

      for (int i = 0; i < 4; i++) {
        user.registerFailedLogin(NOW);
      }

      assertThat(user.failedLoginAttempts()).isEqualTo(4);
      assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void the_fifth_consecutive_failed_attempt_locks_the_account_for_15_minutes() {
      User user = newUser();

      for (int i = 0; i < 5; i++) {
        user.registerFailedLogin(NOW);
      }

      assertThat(user.isLocked(NOW)).isTrue();
      assertThat(user.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
      assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)).minusSeconds(1))).isTrue();
      assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void the_counter_restarts_after_locking_so_the_user_gets_five_new_attempts() {
      User user = newUser();
      for (int i = 0; i < 5; i++) {
        user.registerFailedLogin(NOW);
      }

      assertThat(user.failedLoginAttempts()).isZero();
    }

    @Test
    void a_successful_login_resets_the_counter() {
      User user = newUser();
      user.registerFailedLogin(NOW);
      user.registerFailedLogin(NOW);

      user.registerSuccessfulLogin();

      assertThat(user.failedLoginAttempts()).isZero();
      assertThat(user.lockedUntil()).isNull();
    }

    @Test
    void attempts_are_consecutive_a_success_in_between_starts_over() {
      User user = newUser();
      for (int i = 0; i < 4; i++) {
        user.registerFailedLogin(NOW);
      }
      user.registerSuccessfulLogin();
      for (int i = 0; i < 4; i++) {
        user.registerFailedLogin(NOW);
      }

      assertThat(user.isLocked(NOW)).isFalse();
    }
  }

  @Nested
  class Status {

    @Test
    void only_active_accounts_can_sign_in() {
      User user = newUser();
      assertThat(user.isActive()).isFalse();

      user.verifyEmail(NOW);
      assertThat(user.isActive()).isTrue();

      user.suspend(NOW);
      assertThat(user.isActive()).isFalse();
      assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void reactivating_a_suspended_account_makes_it_active_again() {
      User user = newUser();
      user.verifyEmail(NOW);
      user.suspend(NOW);

      user.reactivate(NOW.plusSeconds(1));

      assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void reactivating_a_non_suspended_account_changes_nothing() {
      User user = newUser();

      user.reactivate(NOW);

      assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }
  }

  @Nested
  class Credentials {

    @Test
    void changing_the_password_replaces_the_hash() {
      User user = newUser();

      user.changePassword(new PasswordHash("$2a$12$nuevo"), NOW.plusSeconds(10));

      assertThat(user.passwordHash().value()).isEqualTo("$2a$12$nuevo");
      assertThat(user.updatedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void the_password_hash_is_never_printed() {
      assertThat(new PasswordHash("$2a$12$secreto").toString()).doesNotContain("secreto");
    }

    @Test
    void the_password_hash_cannot_be_blank() {
      assertThatThrownBy(() -> new PasswordHash(" ")).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void updating_the_profile_replaces_it() {
    User user = newUser();
    Profile profile = new Profile(new DisplayName("Ana P."), "Biobío", "Concepción", null);

    user.updateProfile(profile, NOW.plusSeconds(30));

    assertThat(user.profile()).isEqualTo(profile);
    assertThat(user.updatedAt()).isEqualTo(NOW.plusSeconds(30));
  }

  @Test
  void a_user_can_be_rehydrated_from_storage_without_raising_events() {
    UserId id = UserId.newId();

    User user =
        User.rehydrate(
            id,
            new Email("b@ejemplo.cl"),
            new PasswordHash("$2a$12$x"),
            UserStatus.ACTIVE,
            java.util.Set.of(Role.USER, Role.ADMIN),
            Profile.withName(new DisplayName("Admin")),
            2,
            null,
            NOW,
            NOW,
            7L);

    assertThat(user.id()).isEqualTo(id);
    assertThat(user.isAdmin()).isTrue();
    assertThat(user.failedLoginAttempts()).isEqualTo(2);
    assertThat(user.version()).isEqualTo(7L);
    assertThat(user.pullDomainEvents()).isEmpty();
  }

  @Test
  void user_ids_round_trip_through_text() {
    UserId id = UserId.newId();

    assertThat(UserId.of(id.toString())).isEqualTo(id);
    assertThatThrownBy(() -> UserId.of("no-es-uuid"))
        .isInstanceOf(cl.trato.shared.domain.exception.DomainValidationException.class);
  }
}
