package cl.trato.identity.infrastructure.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.RecordingEmailSender;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Atajos para las pruebas de API de identidad. */
final class IdentityApiClient {

  static final String PASSWORD = "caballo-bateria-grapa";
  static final String COOKIE = "trato_refresh";

  private final MockMvc mvc;
  private final RecordingEmailSender mail;

  IdentityApiClient(MockMvc mvc, RecordingEmailSender mail) {
    this.mvc = mvc;
    this.mail = mail;
  }

  /**
   * Sesión iniciada.
   *
   * @param accessToken JWT de acceso
   * @param refreshToken valor de la cookie de refresco
   */
  record Session(String accessToken, String refreshToken) {
    String bearer() {
      return "Bearer " + accessToken;
    }

    Cookie cookie() {
      return new Cookie(COOKIE, refreshToken);
    }
  }

  static String uniqueEmail() {
    return "u" + UUID.randomUUID().toString().substring(0, 12) + "@ejemplo.cl";
  }

  /** IP de cliente distinta por prueba, para no compartir el balde del límite de login. */
  static String uniqueIp() {
    return "10.0." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
  }

  ResultActions register(String email, String password) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"name":"Ana Pérez","email":"%s","password":"%s","acceptTerms":true}
                """
                    .formatted(email, password)));
  }

  ResultActions login(String email, String password, String ip) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/login")
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
  }

  ResultActions refresh(String refreshToken) throws Exception {
    var request = post("/api/v1/auth/refresh");
    if (refreshToken != null) {
      request.cookie(new Cookie(COOKIE, refreshToken));
    }
    return mvc.perform(request);
  }

  String verificationToken(String email) {
    return mail.awaitToken(email, "Verifica tu correo");
  }

  void verify(String token) throws Exception {
    mvc.perform(
            post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)))
        .andExpect(status().isOk());
  }

  /** Registra, verifica el correo y deja una cuenta ACTIVA. */
  String activeAccount() throws Exception {
    String email = uniqueEmail();
    register(email, PASSWORD).andExpect(status().isAccepted());
    verify(verificationToken(email));
    return email;
  }

  Session signIn(String email) throws Exception {
    return sessionFrom(login(email, PASSWORD, uniqueIp()).andExpect(status().isOk()));
  }

  static Session sessionFrom(ResultActions result) throws Exception {
    MockHttpServletResponse response = result.andReturn().getResponse();
    String accessToken = JsonPath.read(response.getContentAsString(), "$.accessToken");
    Cookie cookie = response.getCookie(COOKIE);
    return new Session(accessToken, cookie == null ? null : cookie.getValue());
  }
}
