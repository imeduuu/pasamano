package cl.trato.catalog.application;

import cl.trato.catalog.application.port.AuditLog;
import cl.trato.catalog.application.port.CategoryListingCount;
import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.domain.Category;
import cl.trato.shared.domain.exception.ConflictException;
import cl.trato.shared.domain.exception.DomainValidationException;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso administrativos de categorías (CU-18, RF-CAT-02, RF-ADM-05). */
public class ManageCategories {
  private final CategoryRepository categories;
  private final AuditLog auditLog;
  private final CategoryListingCount listingCount;
  private final Clock clock;

  public ManageCategories(
      CategoryRepository categories,
      AuditLog auditLog,
      CategoryListingCount listingCount,
      Clock clock) {
    this.categories = categories;
    this.auditLog = auditLog;
    this.listingCount = listingCount;
    this.clock = clock;
  }

  @Transactional
  public CategoryView create(UUID adminId, String name, int order, UUID parentId, String reason) {
    Category parent =
        parentId == null
            ? null
            : categories.findById(parentId).orElseThrow(() -> notFound(parentId));
    Category category = Category.create(name, order, true, parent);
    if (categories.existsBySlug(category.slug())) {
      throw new ConflictException(
          "CATEGORY_SLUG_EXISTS", "Ya existe una categoría con ese nombre.");
    }
    Category saved = categories.save(category);
    auditLog.record(adminId, "CATEGORY_CREATED", saved.id().toString(), reason, clock.instant());
    return view(saved);
  }

  @Transactional
  public CategoryView update(
      UUID adminId,
      UUID id,
      String name,
      Integer order,
      UUID parentId,
      boolean parentSpecified,
      String reason) {
    Category current = categories.findById(id).orElseThrow(() -> notFound(id));
    String updatedName = name == null ? current.name() : name;
    int updatedOrder = order == null ? current.sortOrder() : order;
    UUID updatedParentId = parentSpecified ? parentId : current.parentId();
    Category parent =
        updatedParentId == null
            ? null
            : categories.findById(updatedParentId).orElseThrow(() -> notFound(updatedParentId));
    if (parent != null && parent.id().equals(id)) {
      throw new DomainValidationException(
          "INVALID_CATEGORY_PARENT", "Una categoría no puede ser su propio padre.");
    }
    if (categories.hasChildren(id) && parent != null) {
      throw new DomainValidationException(
          "INVALID_CATEGORY_PARENT", "La jerarquía de categorías solo permite dos niveles.");
    }
    Category updated =
        current.update(updatedName, updatedOrder, parent == null ? null : parent.id());
    if (categories.existsBySlug(updated.slug()) && !updated.slug().equals(current.slug())) {
      throw new ConflictException(
          "CATEGORY_SLUG_EXISTS", "Ya existe una categoría con ese nombre.");
    }
    Category saved = categories.save(updated);
    auditLog.record(adminId, "CATEGORY_UPDATED", id.toString(), reason, clock.instant());
    return view(saved);
  }

  @Transactional
  public void deactivate(UUID adminId, UUID id, String reason) {
    Category current = categories.findById(id).orElseThrow(() -> notFound(id));
    listingCount.count(id);
    categories.save(current.deactivate());
    auditLog.record(adminId, "CATEGORY_DEACTIVATED", id.toString(), reason, clock.instant());
  }

  private static CategoryView view(Category category) {
    return new CategoryView(
        category.id(), category.name(), category.slug(), category.sortOrder(), List.of());
  }

  private static ResourceNotFoundException notFound(UUID id) {
    return new ResourceNotFoundException(
        "CATEGORY_NOT_FOUND", "No se encontró la categoría " + id + ".");
  }
}
