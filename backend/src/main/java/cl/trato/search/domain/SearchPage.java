package cl.trato.search.domain;

import java.util.List;
import java.util.UUID;

/** Publicaciones encontradas y total para la página solicitada. */
public record SearchPage(List<UUID> results, long total) {
  public SearchPage {
    results = List.copyOf(results);
    if (total < 0) {
      throw new IllegalArgumentException("El total no puede ser negativo.");
    }
  }
}
