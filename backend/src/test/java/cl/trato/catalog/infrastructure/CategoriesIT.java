package cl.trato.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Persistencia y API pública de categorías de RF-CAT-01. */
@IntegrationTest
class CategoriesIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired CategoryRepository categories;
  UUID root;

  @BeforeEach
  void seed() {
    jdbc.update("delete from categories where parent_id is not null");
    jdbc.update("delete from categories");
    root = UUID.randomUUID();
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
        root,
        "Hogar",
        "hogar",
        1,
        true);
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active,parent_id) values (?,?,?,?,?,?)",
        UUID.randomUUID(),
        "Cocina",
        "cocina",
        2,
        true,
        root);
    jdbc.update(
        "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
        UUID.randomUUID(),
        "Oculta",
        "oculta",
        0,
        false);
  }

  @Test
  void repository_persists_unique_slugs_and_returns_only_active_categories_in_order() {
    assertThat(categories.findAllActive())
        .extracting(category -> category.name())
        .containsExactly("Hogar", "Cocina");
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                "insert into categories(id,name,slug,sort_order,active) values (?,?,?,?,?)",
                UUID.randomUUID(),
                "Duplicada",
                "hogar",
                3,
                true))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void database_rejects_a_child_of_a_level_two_category() {
    UUID child = jdbc.queryForObject("select id from categories where slug = 'cocina'", UUID.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                "insert into categories(id,name,slug,sort_order,active,parent_id) values (?,?,?,?,?,?)",
                UUID.randomUUID(),
                "Ollas",
                "ollas",
                1,
                true,
                child))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void endpoint_returns_active_tree_without_authentication() throws Exception {
    mvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Hogar"))
        .andExpect(jsonPath("$[0].children[0].slug").value("cocina"))
        .andExpect(jsonPath("$[0].children[0].children").isEmpty())
        .andExpect(jsonPath("$[1]").doesNotExist());
  }
}
