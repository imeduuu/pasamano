package cl.trato.catalog.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import cl.trato.support.RecordingEmailSender;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Pruebas HTTP de HU-08/HU-10 (RF-PUB-01/03/04/06). */
@IntegrationTest
class ListingsApiIT {
  @Autowired MockMvc mvc;
  @Autowired RecordingEmailSender mail;
  @Autowired JdbcTemplate jdbc;

  private record Session(String accessToken) {
    String bearer() {
      return "Bearer " + accessToken;
    }
  }

  private Session seller;
  private UUID category;
  private static final String BODY =
      """
      {"title":"Mesa de comedor usada","description":"Madera maciza","price":15000,
      "stock":2,"condition":"USADO","categoryId":"%s","region":"13","commune":"13101"}
      """;

  @BeforeEach
  void setUp() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    UUID root = UUID.randomUUID();
    category = UUID.randomUUID();
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
        root,
        "Casa " + suffix,
        "casa-" + suffix,
        1,
        true);
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active,parent_id) values (?,?,?,?,?,?)",
        category,
        "Muebles " + suffix,
        "muebles-" + suffix,
        1,
        true,
        root);
    seller = createSeller("seller-" + suffix + "@ejemplo.cl", true, suffix);
  }

  @Test
  void creates_draft_and_supports_versioned_edit_and_soft_delete() throws Exception {
    MvcResult created =
        mvc.perform(
                post("/api/v1/listings")
                    .header("Authorization", seller.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("ETag"))
            .andExpect(jsonPath("$.status").value("BORRADOR"))
            .andReturn();
    UUID id = UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
    String etag = created.getResponse().getHeader("ETag");
    mvc.perform(
            patch("/api/v1/listings/" + id)
                .header("Authorization", seller.bearer())
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("Mesa de comedor usada", "Mesa de comedor renovada")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""));
    mvc.perform(
            delete("/api/v1/listings/" + id)
                .header("Authorization", seller.bearer())
                .header("If-Match", "\"1\""))
        .andExpect(status().isNoContent());
  }

  @Test
  void returns_expected_auth_validation_forbidden_not_found_conflict_and_precondition_statuses()
      throws Exception {
    mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(
            post("/api/v1/listings")
                .header("Authorization", seller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    Session noSeller =
        createSeller(
            "noseller-" + UUID.randomUUID() + "@ejemplo.cl",
            false,
            UUID.randomUUID().toString().substring(0, 8));
    mvc.perform(
            post("/api/v1/listings")
                .header("Authorization", noSeller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SELLER_PROFILE_REQUIRED"));
    UUID missing = UUID.randomUUID();
    mvc.perform(
            delete("/api/v1/listings/" + missing)
                .header("Authorization", seller.bearer())
                .header("If-Match", "\"0\""))
        .andExpect(status().isNotFound());
    MvcResult created =
        mvc.perform(
                post("/api/v1/listings")
                    .header("Authorization", seller.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body()))
            .andExpect(status().isCreated())
            .andReturn();
    UUID id = UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
    Session otherSeller =
        createSeller(
            "other-" + UUID.randomUUID() + "@ejemplo.cl",
            true,
            UUID.randomUUID().toString().substring(0, 8));
    mvc.perform(
            delete("/api/v1/listings/" + id)
                .header("Authorization", otherSeller.bearer())
                .header("If-Match", "\"0\""))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/listings/" + id)
                .header("Authorization", seller.bearer())
                .header("If-Match", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("INVALID_IF_MATCH"));
    mvc.perform(
            patch("/api/v1/listings/" + id)
                .header("Authorization", seller.bearer())
                .header("If-Match", "\"9\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isConflict());
  }

  private String body() {
    return BODY.formatted(category);
  }

  private Session createSeller(String email, boolean activateSeller, String suffix)
      throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana Pérez\",\"email\":\""
                        + email
                        + "\",\"password\":\"caballo-bateria-grapa\",\"acceptTerms\":true}"))
        .andExpect(status().isAccepted());
    String token = mail.awaitToken(email, "Verifica tu correo");
    mvc.perform(
            post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
        .andExpect(status().isOk());
    MvcResult login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "10.1." + (int) (Math.random() * 250) + ".1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + email + "\",\"password\":\"caballo-bateria-grapa\"}"))
            .andExpect(status().isOk())
            .andReturn();
    Session session =
        new Session(JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken"));
    if (activateSeller) {
      mvc.perform(
              post("/api/v1/me/seller-profile")
                  .header("Authorization", session.bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"storeName\":\"Tienda " + suffix + "\",\"acceptSalesTerms\":true}"))
          .andExpect(status().isCreated());
    }
    return session;
  }
}
