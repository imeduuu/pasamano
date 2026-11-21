package cl.trato.identity.application;

import static cl.trato.identity.application.IdentityFakes.GOOD_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.exception.InvalidCredentialsException;
import cl.trato.identity.domain.exception.UserNotFoundException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** RF-USR-01 (ver y editar perfil) y RF-AUT-07 (cambiar contraseña autenticado). */
class ProfileUseCasesTest {

  final IdentityFakes fakes = new IdentityFakes();

  @Test
  void returns_the_profile_of_the_requested_user() {
    User user = fakes.activeUser("ana@ejemplo.cl");

    UserProfileView view = fakes.getProfile().execute(user.id());

    assertThat(view.id()).isEqualTo(user.id().toString());
    assertThat(view.email()).isEqualTo("ana@ejemplo.cl");
    assertThat(view.name()).isEqualTo("Usuaria Prueba");
    assertThat(view.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(view.roles()).containsExactly(Role.USER);
    assertThat(view.createdAt()).isEqualTo(IdentityFakes.START);
  }

  @Test
  void an_unknown_user_is_not_found() {
    assertThatThrownBy(() -> fakes.getProfile().execute(UserId.newId()))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updates_only_the_fields_that_are_sent() {
    User user = fakes.activeUser("ana@ejemplo.cl");
    fakes.clock.advance(Duration.ofMinutes(1));

    fakes
        .updateProfile()
        .execute(new UpdateProfileCommand(user.id(), null, "Valparaíso", "Viña del Mar", null));
    UserProfileView view =
        fakes
            .updateProfile()
            .execute(new UpdateProfileCommand(user.id(), "Ana P.", null, null, null));

    assertThat(view.name()).isEqualTo("Ana P.");
    assertThat(view.region()).isEqualTo("Valparaíso");
    assertThat(view.commune()).isEqualTo("Viña del Mar");
    assertThat(fakes.users.findById(user.id()).orElseThrow().updatedAt())
        .isEqualTo(fakes.clock.instant());
  }

  @Test
  void an_empty_value_clears_an_optional_field() {
    User user = fakes.activeUser("ana@ejemplo.cl");
    fakes
        .updateProfile()
        .execute(
            new UpdateProfileCommand(
                user.id(), null, "Biobío", "Concepción", "https://img.ejemplo.cl/a.png"));

    UserProfileView view =
        fakes.updateProfile().execute(new UpdateProfileCommand(user.id(), null, "", "", ""));

    assertThat(view.region()).isNull();
    assertThat(view.commune()).isNull();
    assertThat(view.photoUrl()).isNull();
  }

  @Test
  void invalid_values_are_rejected() {
    User user = fakes.activeUser("ana@ejemplo.cl");

    assertThatThrownBy(() ->
            fakes
                .updateProfile()
                .execute(new UpdateProfileCommand(user.id(), "A", null, null, null)))
        .extracting("code")
        .isEqualTo("INVALID_NAME");
    assertThatThrownBy(() ->
            fakes
                .updateProfile()
                .execute(new UpdateProfileCommand(user.id(), null, null, null, "javascript:x")))
        .extracting("code")
        .isEqualTo("INVALID_PHOTO_URL");
  }

  @Test
  void changing_the_password_requires_the_current_one_and_revokes_sessions() {
    User user = fakes.activeUser("ana@ejemplo.cl");
    AuthSession session = fakes.login().execute(new LoginCommand("ana@ejemplo.cl", GOOD_PASSWORD));

    fakes
        .changePassword()
        .execute(new ChangePasswordCommand(user.id(), GOOD_PASSWORD, "nueva-clave-segura"));

    assertThat(fakes.refreshTokens.ofUser(user.id()))
        .allSatisfy(token -> assertThat(token.revokedAt()).isNotNull());
    assertThat(session.refreshToken()).isNotBlank();
    assertThatThrownBy(
            () -> fakes.login().execute(new LoginCommand("ana@ejemplo.cl", GOOD_PASSWORD)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void a_wrong_current_password_or_weak_new_password_is_rejected() {
    User user = fakes.activeUser("ana@ejemplo.cl");

    assertThatThrownBy(() ->
            fakes
                .changePassword()
                .execute(new ChangePasswordCommand(user.id(), "no-es-la-actual", "nueva-clave-ok")))
        .extracting("code")
        .isEqualTo("CURRENT_PASSWORD_INCORRECT");
    assertThatThrownBy(() ->
            fakes
                .changePassword()
                .execute(new ChangePasswordCommand(user.id(), GOOD_PASSWORD, "corta")))
        .extracting("code")
        .isEqualTo("PASSWORD_TOO_SHORT");
  }
}
