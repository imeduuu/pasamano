package cl.trato.catalog.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Categoría del catálogo y reglas de jerarquía de RF-CAT-01. */
public record Category(
    UUID id, String name, String slug, int sortOrder, boolean active, UUID parentId) {

  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
  private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

  public static Category create(String name, int sortOrder, boolean active, Category parent) {
    if (name == null || name.isBlank()) {
      throw new DomainValidationException(
          "INVALID_CATEGORY", "El nombre de la categoría es obligatorio.");
    }
    if (sortOrder < 0) {
      throw new DomainValidationException("INVALID_CATEGORY", "El orden no puede ser negativo.");
    }
    if (parent != null && parent.parentId() != null) {
      throw new DomainValidationException(
          "INVALID_CATEGORY", "Una categoría de nivel 2 no puede tener hijas.");
    }
    String slug = slugify(name);
    if (slug.isBlank()) {
      throw new DomainValidationException(
          "INVALID_CATEGORY", "El nombre debe producir un slug válido.");
    }
    return new Category(
        UUID.randomUUID(),
        name.strip(),
        slug,
        sortOrder,
        active,
        parent == null ? null : parent.id());
  }

  public Category update(String newName, int newSortOrder, UUID newParentId) {
    Category updated = create(newName, newSortOrder, active, null);
    return new Category(id, updated.name(), updated.slug(), newSortOrder, active, newParentId);
  }

  public Category deactivate() {
    return new Category(id, name, slug, sortOrder, false, parentId);
  }

  private static String slugify(String value) {
    String normalized =
        Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    return NON_SLUG
        .matcher(DIACRITICS.matcher(normalized).replaceAll(""))
        .replaceAll("-")
        .replaceAll("^-|-$", "");
  }
}
