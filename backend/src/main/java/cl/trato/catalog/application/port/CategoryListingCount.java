package cl.trato.catalog.application.port;

import java.util.UUID;

/** Puerto temporal para contar publicaciones asociadas a una categoría. */
public interface CategoryListingCount {
  long count(UUID categoryId);
}
