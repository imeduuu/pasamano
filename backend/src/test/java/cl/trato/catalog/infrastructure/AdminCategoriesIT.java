package cl.trato.catalog.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

/** API administrativa de categorías (CU-18, RF-CAT-02, RF-ADM-05). */
@IntegrationTest
class AdminCategoriesIT {
  @Autowired MockMvc mvc;
  @Autowired JwtEncoder encoder;
  @Autowired JdbcTemplate jdbc;

  @Test
  void admin_creates_updates_and_deactivates_categories_and_audits_each_action() throws Exception {
    String bearer = bearer("ADMIN");
    UUID parentId = UUID.randomUUID();
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
        parentId,
        "Padre de prueba",
        "padre-" + parentId,
        0,
        true);
    mvc.perform(
            post("/api/v1/admin/categories")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jardín\",\"sortOrder\":3,\"parentId\":\"" + parentId + "\"}"))
        .andExpect(status().isCreated());
    UUID id = jdbc.queryForObject("select id from categories where slug='jardin'", UUID.class);
    mvc.perform(
            patch("/api/v1/admin/categories/" + id)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Patio\",\"sortOrder\":4}"))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/v1/admin/categories/" + id)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":null}"))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select parent_id from categories where id=?", UUID.class, id))
        .isNull();
    mvc.perform(
            patch("/api/v1/admin/categories/" + id)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":\"" + id + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/admin/categories/" + id + "/deactivate").header("Authorization", bearer))
        .andExpect(status().isNoContent());
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from audit_log", Integer.class))
        .isEqualTo(4);
  }

  @Test
  void validates_authentication_input_missing_resources_and_duplicate_slug() throws Exception {
    mvc.perform(
            post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/admin/categories")
                .header("Authorization", bearer("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Casa\",\"sortOrder\":0}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/admin/categories")
                .header("Authorization", bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \",\"sortOrder\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/v1/admin/categories/" + UUID.randomUUID())
                .header("Authorization", bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Casa\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/admin/categories")
                .header("Authorization", bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Casa\",\"sortOrder\":0}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/admin/categories")
                .header("Authorization", bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CASA\",\"sortOrder\":0}"))
        .andExpect(status().isConflict());
  }

  private String bearer(String role) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("trato")
            .subject(UUID.randomUUID().toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .claim("roles", List.of(role))
            .build();
    return "Bearer "
        + encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
  }
}
