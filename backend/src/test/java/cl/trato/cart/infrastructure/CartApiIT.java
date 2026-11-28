package cl.trato.cart.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import cl.trato.support.RecordingEmailSender;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-15: contrato HTTP, RF-CAR-04 y aislamiento por comprador. */
@IntegrationTest
class CartApiIT {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RecordingEmailSender mail;
  private UUID leaf;
  private UUID root;
  private String suffix;
  private Session seller;
  private Session buyer;

  private record Session(String token) {
    String bearer() {
      return "Bearer " + token;
    }
  }

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().substring(0, 8);
    root = UUID.randomUUID();
    leaf = UUID.randomUUID();
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
        root,
        "Hogar " + suffix,
        "hogar-" + suffix,
        1,
        true);
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active,parent_id) values (?,?,?,?,?,?)",
        leaf,
        "Cocina " + suffix,
        "cocina-" + suffix,
        1,
        true,
        root);
    seller = user("cart-seller-" + suffix + "@ejemplo.cl");
    buyer = user("cart-buyer-" + suffix + "@ejemplo.cl");
    mvc.perform(
            post("/api/v1/me/seller-profile")
                .header("Authorization", seller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeName\":\"Tienda " + suffix + "\",\"acceptSalesTerms\":true}"))
        .andExpect(status().isCreated());
  }

  @AfterEach
  void cleanup() {
    if (suffix == null) {
      return;
    }
    String sellerEmail = "cart-seller-" + suffix + "@ejemplo.cl";
    String buyerEmail = "cart-buyer-" + suffix + "@ejemplo.cl";
    jdbc.update(
        "delete from carts where buyer_id in (select id from users where email in (?, ?))",
        sellerEmail,
        buyerEmail);
    jdbc.update(
        "delete from listings where seller_id in (select id from users where email = ?)",
        sellerEmail);
    jdbc.update("delete from users where email in (?, ?)", sellerEmail, buyerEmail);
    jdbc.update("delete from categories where id = ?", leaf);
    jdbc.update("delete from categories where id = ?", root);
  }

  @Test
  void HU15_cart_scoped_to_authenticated_buyer_and_handles_success_and_conflicts()
      throws Exception {
    mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
    UUID id = listing(2);
    mvc.perform(
            put("/api/v1/cart/items/" + id)
                .header("Authorization", buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(15000));
    mvc.perform(get("/api/v1/cart").header("Authorization", seller.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    mvc.perform(
            put("/api/v1/cart/items/" + id)
                .header("Authorization", seller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("OWN_LISTING"));
    mvc.perform(
            put("/api/v1/cart/items/" + id)
                .header("Authorization", buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
        .andExpect(jsonPath("$.maximumAvailable").value(2));
    mvc.perform(
            put("/api/v1/cart/items/" + UUID.randomUUID())
                .header("Authorization", buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"));
    mvc.perform(delete("/api/v1/cart/items/" + id).header("Authorization", buyer.bearer()))
        .andExpect(status().isNoContent());
  }

  @Test
  void unavailable_items_are_marked_and_excluded_from_total() throws Exception {
    UUID id = listing(2);
    mvc.perform(
            put("/api/v1/cart/items/" + id)
                .header("Authorization", buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk());
    jdbc.update("update listings set status='PAUSADA' where id=?", id);
    mvc.perform(
            put("/api/v1/cart/items/" + id)
                .header("Authorization", buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LISTING_UNAVAILABLE"));
    mvc.perform(get("/api/v1/cart").header("Authorization", buyer.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.sellers[0].items[0].available").value(false))
        .andExpect(jsonPath("$.warnings[0]").value("PAUSED"));
  }

  private UUID listing(int stock) throws Exception {
    String body =
        "{\"title\":\"Mesa de comedor usada\",\"description\":\"Madera\",\"price\":15000,\"stock\":"
            + stock
            + ",\"condition\":\"USADO\",\"categoryId\":\""
            + leaf
            + "\",\"region\":\"13\",\"commune\":\"13101\"}";
    MvcResult created =
        mvc.perform(
                post("/api/v1/listings")
                    .header("Authorization", seller.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    UUID id = UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
    jdbc.update(
        "insert into listing_images(id,listing_id,position,image_reference,thumbnail_reference) values (?,?,?,?,?)",
        UUID.randomUUID(),
        id,
        0,
        "listings/cart/test.jpg",
        "listings/cart/thumb.jpg");
    mvc.perform(
            post("/api/v1/listings/" + id + "/publish")
                .header("Authorization", seller.bearer())
                .header("If-Match", "\"0\""))
        .andExpect(status().isOk());
    return id;
  }

  private Session user(String email) throws Exception {
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
                    .header("X-Forwarded-For", "10.2." + (int) (Math.random() * 250) + ".1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + email + "\",\"password\":\"caballo-bateria-grapa\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return new Session(JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken"));
  }
}
