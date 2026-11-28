package cl.trato.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.support.IntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica búsqueda textual y sus índices PostgreSQL (HU-12, ADR-004). */
@IntegrationTest
class ListingSearchIndexIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired CategoryRepository categories;
  @Autowired UserRepository users;

  @Test
  void finds_unaccented_text_and_uses_search_indexes() {
    UUID id = UUID.randomUUID();
    String suffix = id.toString().substring(0, 8);
    Category category = categories.save(Category.create("Categoría " + suffix, 1, true, null));
    User seller =
        users.save(
            User.register(
                UserId.newId(),
                new Email("seller-" + suffix + "@ejemplo.cl"),
                new PasswordHash("$2a$12$hash"),
                new DisplayName("Vendedor"),
                Instant.now()));
    jdbc.update(
        "INSERT INTO listings (id, title, description, price, stock_total, stock_reserved, "
            + "condition, category_id, region, commune, seller_id, status) "
            + "VALUES (?, 'Cámara', 'Equipo fotográfico', 15000, 1, 0, 'NUEVO', ?, "
            + "'Metropolitana', 'Santiago', ?, 'ACTIVA')",
        id,
        category.id(),
        seller.id().value());

    List<UUID> matches =
        jdbc.query(
            "SELECT id FROM listings WHERE search_vector @@ plainto_tsquery('spanish', ?)",
            (rs, row) -> UUID.fromString(rs.getString("id")),
            "camara");
    assertThat(matches).contains(id);

    jdbc.execute("SET enable_seqscan = off");
    List<String> plan =
        jdbc.query(
            "EXPLAIN SELECT id FROM listings WHERE search_vector @@ "
                + "plainto_tsquery('spanish', 'camara')",
            (rs, row) -> rs.getString(1));
    jdbc.execute("SET enable_seqscan = on");

    assertThat(String.join(" ", plan)).contains("ix_listings_search_vector");
  }
}
