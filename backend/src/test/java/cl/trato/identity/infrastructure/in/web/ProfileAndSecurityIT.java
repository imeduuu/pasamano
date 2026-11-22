package cl.trato.identity.infrastructure.in.web;

import static cl.trato.identity.infrastructure.in.web.IdentityApiClient.PASSWORD;
import static cl.trato.identity.infrastructure.in.web.IdentityApiClient.uniqueIp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import cl.trato.support.RecordingEmailSender;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Perfil propio (RF-USR-01) y pruebas de seguridad: aislamiento entre usuarios (RNF-SEG-08),
 * reutilización de tokens de refresco (RF-AUT-04), roles (RF-AUT-09) y JWT inválidos.
 */
@IntegrationTest
class ProfileAndSecurityIT {

  @Autowired MockMvc mvc;
  @Autowired RecordingEmailSender mail;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired JdbcTemplate jdbc;

  IdentityApiClient api;

  @BeforeEach
  void setUp() {
    api = new IdentityApiClient(mvc, mail);
  }

  @Test
  void the_profile_requires_an_access_token() throws Exception {
    mvc.perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void a_user_sees_and_edits_their_own_profile() throws Exception {
    String email = api.activeAccount();
    IdentityApiClient.Session session = api.signIn(email);

    mvc.perform(get("/api/v1/me").header("Authorization", session.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.name").value("Ana Pérez"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.roles[0]").value("USER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana P.\",\"region\":\"Valparaíso\",\"commune\":\"Viña del Mar\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ana P."))
        .andExpect(jsonPath("$.region").value("Valparaíso"))
        .andExpect(jsonPath("$.commune").value("Viña del Mar"));
  }

  @Test
  void invalid_profile_data_answers_400() throws Exception {
    IdentityApiClient.Session session = api.signIn(api.activeAccount());

    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"photoUrl\":\"javascript:alert(1)\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PHOTO_URL"));
  }

  @Test
  void seller_profile_activation_returns_201_and_requires_valid_store_name_and_terms()
      throws Exception {
    IdentityApiClient.Session session = api.signIn(api.activeAccount());

    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"Tienda Sur\",\"acceptSalesTerms\":true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.storeName").value("Tienda Sur"))
        .andExpect(jsonPath("$.termsVersion").value("2026-11-20"))
        .andExpect(jsonPath("$.termsAcceptedAt").isNotEmpty());
  }

  @Test
  void seller_profile_activation_returns_400_for_invalid_input() throws Exception {
    IdentityApiClient.Session session = api.signIn(api.activeAccount());

    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"ab\",\"acceptSalesTerms\":true}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"Tienda Sur\",\"acceptSalesTerms\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void seller_profile_requires_authentication_and_conflicts_when_already_activated()
      throws Exception {
    mvc.perform(
            post("/api/v1/me/seller-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"Tienda Sur\",\"acceptSalesTerms\":true}"))
        .andExpect(status().isUnauthorized());

    IdentityApiClient.Session session = api.signIn(api.activeAccount());
    String body = "{\"storeName\":\"Tienda Sur\",\"acceptSalesTerms\":true}";
    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void a_user_cannot_read_or_edit_another_users_profile() throws Exception {
    String anaEmail = api.activeAccount();
    String betoEmail = api.activeAccount();
    IdentityApiClient.Session ana = api.signIn(anaEmail);
    IdentityApiClient.Session beto = api.signIn(betoEmail);
    String betoId =
        JsonPath.read(
            mvc.perform(get("/api/v1/me").header("Authorization", beto.bearer()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    // Ana intenta apuntar al perfil de Beto: el id y el correo del cuerpo se ignoran.
    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", ana.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"id\":\"%s\",\"email\":\"%s\",\"name\":\"Hackeado\"}"
                        .formatted(betoId, betoEmail)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(anaEmail));
    mvc.perform(get("/api/v1/me/" + betoId).header("Authorization", ana.bearer()))
        .andExpect(status().isNotFound());

    mvc.perform(get("/api/v1/me").header("Authorization", beto.bearer()))
        .andExpect(jsonPath("$.name").value("Ana Pérez"))
        .andExpect(jsonPath("$.email").value(betoEmail));
  }

  @Test
  void reusing_a_rotated_refresh_token_revokes_the_whole_family() throws Exception {
    IdentityApiClient.Session first = api.signIn(api.activeAccount());
    IdentityApiClient.Session second =
        IdentityApiClient.sessionFrom(api.refresh(first.refreshToken()).andExpect(status().isOk()));

    api.refresh(first.refreshToken())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

    api.refresh(second.refreshToken()).andExpect(status().isUnauthorized());
    Integer active =
        jdbc.queryForObject(
            "select count(*) from refresh_tokens t join refresh_tokens f on f.family_id ="
                + " t.family_id where t.token_hash = encode(sha256(convert_to(?, 'UTF8')), 'hex')"
                + " and f.revoked_at is null",
            Integer.class,
            second.refreshToken());
    assertThat(active).isZero();
  }

  @Test
  void refresh_tokens_are_stored_only_as_hashes() throws Exception {
    IdentityApiClient.Session session = api.signIn(api.activeAccount());

    Integer plain =
        jdbc.queryForObject(
            "select count(*) from refresh_tokens where token_hash = ?",
            Integer.class,
            session.refreshToken());
    assertThat(plain).isZero();
  }

  @Test
  void passwords_are_stored_with_bcrypt_cost_12() throws Exception {
    String email = api.activeAccount();

    String hash =
        jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
    assertThat(hash).startsWith("$2a$12$").doesNotContain(PASSWORD);
  }

  @Test
  void a_regular_user_cannot_reach_admin_endpoints() throws Exception {
    IdentityApiClient.Session session = api.signIn(api.activeAccount());

    mvc.perform(get("/api/v1/admin/audit-log").header("Authorization", session.bearer()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void a_token_signed_with_another_key_is_rejected() throws Exception {
    JwtEncoder forger =
        new NimbusJwtEncoder(
            new ImmutableSecret<>(
                new SecretKeySpec(
                    "otra-clave-cualquiera-de-32-bytes-o-mas".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256")));

    mvc.perform(
            get("/api/v1/me")
                .header("Authorization", "Bearer " + token(forger, Instant.now().plusSeconds(600))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void an_expired_access_token_is_rejected() throws Exception {
    mvc.perform(
            get("/api/v1/me")
                .header(
                    "Authorization",
                    "Bearer " + token(jwtEncoder, Instant.now().minusSeconds(3600))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changing_the_password_requires_the_current_one_and_closes_sessions() throws Exception {
    String email = api.activeAccount();
    IdentityApiClient.Session session = api.signIn(email);

    mvc.perform(
            post("/api/v1/me/password")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"no-es-la-actual\",\"newPassword\":\"nueva-clave-ok-1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INCORRECT"));

    mvc.perform(
            post("/api/v1/me/password")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"%s\",\"newPassword\":\"nueva-clave-ok-1\"}"
                        .formatted(PASSWORD)))
        .andExpect(status().isNoContent());

    api.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    api.login(email, "nueva-clave-ok-1", uniqueIp()).andExpect(status().isOk());
  }

  private static String token(JwtEncoder encoder, Instant expiresAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("trato")
            .subject("00000000-0000-0000-0000-000000000001")
            .issuedAt(expiresAt.minusSeconds(900))
            .expiresAt(expiresAt)
            .claim("roles", List.of("USER", "ADMIN"))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
