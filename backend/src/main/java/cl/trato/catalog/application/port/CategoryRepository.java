package cl.trato.catalog.application.port;

import cl.trato.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para consultar y administrar categorías persistidas del catálogo. */
public interface CategoryRepository {
  List<Category> findAllActive();

  Optional<Category> findById(UUID id);

  boolean existsBySlug(String slug);

  boolean hasChildren(UUID id);

  Category save(Category category);
}
