package cl.trato.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Consulta del árbol activo de RF-CAT-01. */
class GetCategoriesTest {
  @Test
  void returns_roots_with_their_active_children() {
    Category root = Category.create("Hogar", 1, true, null);
    Category child = Category.create("Cocina", 1, true, root);
    GetCategories query = new GetCategories(() -> List.of(root, child));

    assertThat(query.execute())
        .containsExactly(
            new CategoryView(
                root.id(),
                "Hogar",
                "hogar",
                1,
                List.of(new CategoryView(child.id(), "Cocina", "cocina", 1, List.of()))));
  }

  @Test
  void returns_empty_tree_when_no_categories_are_active() {
    Category inactive = Category.create("Oculta", 1, false, null);
    CategoryRepository repository = () -> List.of(inactive);

    assertThat(new GetCategories(repository).execute()).isEmpty();
  }
}
