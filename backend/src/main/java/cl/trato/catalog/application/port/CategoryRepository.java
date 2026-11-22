package cl.trato.catalog.application.port;

import cl.trato.catalog.domain.Category;
import java.util.List;

/** Puerto de lectura para categorías activas del catálogo. */
public interface CategoryRepository {
  List<Category> findAllActive();
}
