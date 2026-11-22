package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
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

  @Override
  public Optional<Category> findById(java.util.UUID id) {
    return jpa.findById(id).map(JpaCategoryRepositoryAdapter::toDomain);
  }

  @Override
  public boolean existsBySlug(String slug) {
    return jpa.existsBySlug(slug);
  }

  @Override
  public boolean hasChildren(java.util.UUID id) {
    return jpa.existsByParentId(id);
  }

  @Override
  public Category save(Category category) {
    CategoryJpaEntity row = jpa.findById(category.id()).orElseGet(CategoryJpaEntity::new);
    row.setId(category.id());
    row.setName(category.name());
    row.setSlug(category.slug());
    row.setSortOrder(category.sortOrder());
    row.setActive(category.active());
    row.setParentId(category.parentId());
    return toDomain(jpa.save(row));
  }

  private static Category toDomain(CategoryJpaEntity row) {
    return new Category(
        row.getId(),
        row.getName(),
        row.getSlug(),
        row.getSortOrder(),
        row.isActive(),
        row.getParentId());
  }
}
