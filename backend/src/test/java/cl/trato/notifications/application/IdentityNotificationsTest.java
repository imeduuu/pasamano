package cl.trato.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.event.PasswordResetRequested;
import cl.trato.identity.domain.event.RegistrationAttemptedWithExistingEmail;
import cl.trato.identity.domain.event.VerificationEmailRequested;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RF-NOT-01: correos transaccionales de verificación y recuperación, en español. */
class IdentityNotificationsTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");

  final List<EmailMessage> sent = new ArrayList<>();
  final IdentityNotifications notifications =
      new IdentityNotifications(
          new EmailDelivery(sent::add, new RetryPolicy(1, Duration.ZERO, 1), d -> {}),
          new IdentityEmailTemplates("http://localhost:4200/"));

  @Test
  void the_verification_email_links_to_the_frontend_with_the_token() {
    notifications.on(
        new VerificationEmailRequested(
            UserId.newId(),
            new Email("ana@ejemplo.cl"),
            "Ana",
            "abc_123-XYZ",
            NOW.plus(Duration.ofHours(24)),
            NOW));

    assertThat(sent)
        .singleElement()
        .satisfies(message -> {
          assertThat(message.to()).isEqualTo("ana@ejemplo.cl");
          assertThat(message.subject()).isEqualTo("Verifica tu correo en Trato");
          assertThat(message.textBody())
              .contains("Hola, Ana")
              .contains("http://localhost:4200/verificar-correo?token=abc_123-XYZ")
              .contains("24 horas");
          assertThat(message.htmlBody())
              .contains("href=\"http://localhost:4200/verificar-correo?token=abc_123-XYZ\"");
        });
  }

  @Test
  void the_password_reset_email_is_valid_for_one_hour() {
    notifications.on(
        new PasswordResetRequested(
            UserId.newId(),
            new Email("ana@ejemplo.cl"),
            "Ana",
            "tok",
            NOW.plus(Duration.ofHours(1)),
            NOW));

    assertThat(sent)
        .singleElement()
        .satisfies(message -> {
          assertThat(message.subject()).isEqualTo("Restablece tu contraseña de Trato");
          assertThat(message.textBody())
              .contains("http://localhost:4200/restablecer-contrasena?token=tok")
              .contains("1 hora")
              .contains("Si no lo pediste");
        });
  }

  @Test
  void the_owner_is_told_about_a_registration_attempt_without_any_link_to_act_on() {
    notifications.on(
        new RegistrationAttemptedWithExistingEmail(new Email("ana@ejemplo.cl"), "Ana", NOW));

    assertThat(sent)
        .singleElement()
        .satisfies(message -> {
          assertThat(message.subject()).isEqualTo("Intento de registro con tu correo");
          assertThat(message.textBody())
              .contains("ya tiene una cuenta")
              .contains("http://localhost:4200/recuperar-contrasena")
              .doesNotContain("token=");
        });
  }

  @Test
  void names_are_escaped_in_html() {
    notifications.on(
        new VerificationEmailRequested(
            UserId.newId(),
            new Email("ana@ejemplo.cl"),
            "<script>alert('x')</script>",
            "tok",
            NOW,
            NOW));

    assertThat(sent.getFirst().htmlBody())
        .doesNotContain("<script>")
        .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
  }
}
