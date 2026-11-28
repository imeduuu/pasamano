package cl.trato.search.domain;

import java.util.List;

/** Publicaciones encontradas y total para la página solicitada. */
public record SearchPage(List<SearchListing> results, long total) {
  public SearchPage {
    results = List.copyOf(results);
    if (total < 0) {
      throw new IllegalArgumentException("El total no puede ser negativo.");
    }
  }
}
