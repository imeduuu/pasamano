package cl.trato.catalog.application;

import java.util.List;
import java.util.UUID;

/** Vista pública de una categoría. */
public record CategoryView(
    UUID id, String name, String slug, int sortOrder, List<CategoryView> children) {}
