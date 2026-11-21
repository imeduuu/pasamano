package cl.trato.identity.infrastructure.in.web;

import static cl.trato.identity.infrastructure.in.web.IdentityApiClient.PASSWORD;
import static cl.trato.identity.infrastructure.in.web.IdentityApiClient.uniqueEmail;
import static cl.trato.identity.infrastructure.in.web.IdentityApiClient.uniqueIp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import cl.trato.support.RecordingEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** API de autenticación: CU-01, CU-02 y CU-16 de punta a punta, con PostgreSQL real. */
@IntegrationTest
class AuthApiIT {

  @Autowired MockMvc mvc;
  @Autowired RecordingEmailSender mail;

  IdentityApiClient api;

  @BeforeEach
  void setUp() {
    api = new IdentityApiClient(mvc, mail);
  }

  @Nested
  class Register {

    @Test
    void creates_the_account_and_sends_the_verification_email() throws Exception {
      String email = uniqueEmail();

      api.register(email, PASSWORD)
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.message").value(AuthController.VERIFICATION_SENT));

      assertThat(mail.await(email, "Verifica tu correo").textBody())
          .contains("/verificar-correo?token=");
    }

    @Test
    void an_existing_email_gets_the_same_answer_and_the_owner_is_notified() throws Exception {
      String email = api.activeAccount();

      api.register(email.toUpperCase(), PASSWORD)
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.message").value(AuthController.VERIFICATION_SENT));

      assertThat(mail.await(email, "Intento de registro").textBody())
          .contains("ya tiene una cuenta");
    }

    @Test
    void invalid_data_answers_400_with_errors_next_to_each_field() throws Exception {
      mvc.perform(
              post("/api/v1/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"name\":\"A\",\"email\":\"no-es-correo\",\"password\":\"corta\","
                          + "\"acceptTerms\":false}"))
          .andExpect(status().isBadRequest())
          .andExpect(header().string("Content-Type", containsString("application/problem+json")))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(
              jsonPath("$.errors[*].field")
                  .value(hasItems("name", "email", "password", "acceptTerms")))
          .andExpect(
              jsonPath("$.errors[?(@.field=='acceptTerms')].message")
                  .value(hasItems("Debes aceptar los términos y la política de privacidad.")));
    }

    @Test
    void a_common_password_is_rejected_with_a_stable_code() throws Exception {
      api.register(uniqueEmail(), "password1234")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PASSWORD_TOO_COMMON"))
          .andExpect(jsonPath("$.detail").value(containsString("demasiado común")));
    }

    @Test
    void a_malformed_body_answers_400() throws Exception {
      mvc.perform(
              post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
  }

  @Nested
  class Verify {

    @Test
    void the_link_activates_the_account_only_once() throws Exception {
      String email = uniqueEmail();
      api.register(email, PASSWORD);
      String token = api.verificationToken(email);

      api.verify(token);

      mvc.perform(
              post("/api/v1/auth/verify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\":\"%s\"}".formatted(token)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("TOKEN_ALREADY_USED"));
    }

    @Test
    void an_unknown_token_answers_400() throws Exception {
      mvc.perform(
              post("/api/v1/auth/verify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\":\"inventado\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void a_pending_account_can_ask_for_a_new_link() throws Exception {
      String email = uniqueEmail();
      api.register(email, PASSWORD);
      mail.await(email, "Verifica tu correo");

      mvc.perform(
              post("/api/v1/auth/verify/resend")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"%s\"}".formatted(email)))
          .andExpect(status().isAccepted());

      org.awaitility.Awaitility.await().until(() -> mail.to(email).size() == 2);
    }
  }

  @Nested
  class Login {

    @Test
    void returns_a_15_minute_access_token_and_a_secure_refresh_cookie() throws Exception {
      String email = api.activeAccount();

      MvcResult result =
          api.login(email, PASSWORD, uniqueIp())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.tokenType").value("Bearer"))
              .andExpect(jsonPath("$.expiresIn").value(900))
              .andExpect(jsonPath("$.accessToken").value(notNullValue()))
              .andExpect(jsonPath("$.refreshToken").doesNotExist())
              .andExpect(header().string("Cache-Control", "no-store"))
              .andReturn();

      String setCookie = result.getResponse().getHeader("Set-Cookie");
      assertThat(setCookie)
          .startsWith("trato_refresh=")
          .contains("HttpOnly")
          .contains("Secure")
          .contains("SameSite=Strict")
          .contains("Path=/api/v1/auth")
          .contains("Max-Age=604800");
    }

    @Test
    void wrong_credentials_answer_401_with_a_generic_message() throws Exception {
      String email = api.activeAccount();

      api.login(email, "otra-clave-cualquiera", uniqueIp())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
          .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
      api.login(uniqueEmail(), PASSWORD, uniqueIp())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void an_unverified_account_answers_403() throws Exception {
      String email = uniqueEmail();
      api.register(email, PASSWORD);

      api.login(email, PASSWORD, uniqueIp())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void the_fifth_failed_attempt_locks_the_account_for_15_minutes() throws Exception {
      String email = api.activeAccount();
      for (int i = 0; i < 4; i++) {
        api.login(email, "clave-equivocada", uniqueIp()).andExpect(status().isUnauthorized());
      }

      api.login(email, "clave-equivocada", uniqueIp())
          .andExpect(status().isLocked())
          .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
      api.login(email, PASSWORD, uniqueIp())
          .andExpect(status().isLocked())
          .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void more_than_5_attempts_per_minute_from_the_same_ip_and_email_answer_429() throws Exception {
      String email = uniqueEmail();
      String ip = uniqueIp();
      for (int i = 0; i < 5; i++) {
        api.login(email, "clave-equivocada", ip).andExpect(status().isUnauthorized());
      }

      api.login(email, "clave-equivocada", ip)
          .andExpect(status().isTooManyRequests())
          .andExpect(header().string("Retry-After", notNullValue()))
          .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
      api.login(email, "clave-equivocada", uniqueIp()).andExpect(status().isUnauthorized());
    }

    @Test
    void missing_fields_answer_400() throws Exception {
      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"\",\"password\":\"\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
  }

  @Nested
  class Refresh {

    @Test
    void rotates_the_refresh_token_and_returns_a_new_access_token() throws Exception {
      IdentityApiClient.Session first = api.signIn(api.activeAccount());

      IdentityApiClient.Session second =
          IdentityApiClient.sessionFrom(
              api.refresh(first.refreshToken()).andExpect(status().isOk()));

      assertThat(second.refreshToken()).isNotBlank().isNotEqualTo(first.refreshToken());
      assertThat(second.accessToken()).isNotBlank();
    }

    @Test
    void without_a_cookie_answers_401_and_clears_the_cookie() throws Exception {
      api.refresh(null)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"))
          .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
  }

  @Nested
  class Logout {

    @Test
    void revokes_the_refresh_token_and_clears_the_cookie() throws Exception {
      IdentityApiClient.Session session = api.signIn(api.activeAccount());

      mvc.perform(
              post("/api/v1/auth/logout")
                  .header("Authorization", session.bearer())
                  .cookie(session.cookie()))
          .andExpect(status().isNoContent())
          .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

      api.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void requires_an_access_token() throws Exception {
      mvc.perform(post("/api/v1/auth/logout"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
  }

  @Nested
  class PasswordRecovery {

    @Test
    void forgot_password_answers_the_same_for_existing_and_unknown_emails() throws Exception {
      String existing = api.activeAccount();

      String known =
          mvc.perform(
                  post("/api/v1/auth/password/forgot")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"email\":\"%s\"}".formatted(existing)))
              .andExpect(status().isAccepted())
              .andReturn()
              .getResponse()
              .getContentAsString();
      String unknown =
          mvc.perform(
                  post("/api/v1/auth/password/forgot")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"email\":\"%s\"}".formatted(uniqueEmail())))
              .andExpect(status().isAccepted())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(known).isEqualTo(unknown).contains(AuthController.RESET_SENT);
      assertThat(mail.await(existing, "Restablece").textBody())
          .contains("/restablecer-contrasena?token=");
    }

    @Test
    void reset_changes_the_password_and_revokes_every_refresh_token() throws Exception {
      String email = api.activeAccount();
      IdentityApiClient.Session session = api.signIn(email);
      mvc.perform(
          post("/api/v1/auth/password/forgot")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"email\":\"%s\"}".formatted(email)));
      String token = mail.awaitToken(email, "Restablece");

      mvc.perform(
              post("/api/v1/auth/password/reset")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"token\":\"%s\",\"password\":\"otra-clave-muy-segura\"}".formatted(token)))
          .andExpect(status().isOk());

      api.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
      api.login(email, PASSWORD, uniqueIp()).andExpect(status().isUnauthorized());
      api.login(email, "otra-clave-muy-segura", uniqueIp()).andExpect(status().isOk());
      mvc.perform(
              post("/api/v1/auth/password/reset")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"token\":\"%s\",\"password\":\"una-tercera-clave\"}".formatted(token)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("TOKEN_ALREADY_USED"));
    }

    @Test
    void the_new_password_must_follow_the_policy() throws Exception {
      mvc.perform(
              post("/api/v1/auth/password/reset")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\":\"x\",\"password\":\"corta\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("password"))
          .andExpect(jsonPath("$.code").value(not("INTERNAL_ERROR")));
    }
  }
}
