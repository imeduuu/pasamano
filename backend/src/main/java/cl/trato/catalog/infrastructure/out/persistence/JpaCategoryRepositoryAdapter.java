package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adaptador de lectura JPA del catálogo. */
@Component
class JpaCategoryRepositoryAdapter implements CategoryRepository {
  private final CategoryJpaRepository jpa;

  JpaCategoryRepositoryAdapter(CategoryJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<Category> findAllActive() {
    return jpa.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
        .map(row ->
            new Category(
                row.getId(),
                row.getName(),
                row.getSlug(),
                row.getSortOrder(),
                row.isActive(),
                row.getParentId()))
        .toList();
  }
}
