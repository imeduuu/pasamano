package cl.trato.catalog.infrastructure.in.web;

import cl.trato.catalog.application.CategoryView;
import cl.trato.catalog.application.GetCategories;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública del árbol de categorías (RF-CAT-01). */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categorías")
class CategoriesController {
  private final GetCategories getCategories;

  CategoriesController(GetCategories getCategories) {
    this.getCategories = getCategories;
  }

  @GetMapping
  List<CategoryView> get() {
    return getCategories.execute();
  }
}
