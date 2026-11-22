package cl.trato.catalog.application;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import java.util.ArrayList;
import java.util.List;

/** Obtiene el árbol activo del catálogo (RF-CAT-01). */
public class GetCategories {
  private final CategoryRepository categories;

  public GetCategories(CategoryRepository categories) {
    this.categories = categories;
  }

  public List<CategoryView> execute() {
    List<Category> active = categories.findAllActive().stream().filter(Category::active).toList();
    return active.stream()
        .filter(category -> category.parentId() == null)
        .map(root -> view(root, active))
        .toList();
  }

  private static CategoryView view(Category category, List<Category> all) {
    List<CategoryView> children = new ArrayList<>();
    for (Category child : all) {
      if (category.id().equals(child.parentId())) {
        children.add(view(child, List.of()));
      }
    }
    return new CategoryView(
        category.id(), category.name(), category.slug(), category.sortOrder(), children);
  }
}
