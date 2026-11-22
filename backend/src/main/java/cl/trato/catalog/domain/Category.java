package cl.trato.catalog.domain;

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
      throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("El orden no puede ser negativo.");
    }
    if (parent != null && parent.parentId() != null) {
      throw new IllegalArgumentException("Una categoría de nivel 2 no puede tener hijas.");
    }
    String slug = slugify(name);
    if (slug.isBlank()) {
      throw new IllegalArgumentException("El nombre debe producir un slug válido.");
    }
    return new Category(
        UUID.randomUUID(),
        name.strip(),
        slug,
        sortOrder,
        active,
        parent == null ? null : parent.id());
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
