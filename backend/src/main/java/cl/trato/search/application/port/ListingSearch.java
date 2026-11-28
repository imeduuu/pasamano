package cl.trato.search.application.port;

import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchPage;
import java.util.UUID;

/** Puerto de búsqueda de publicaciones (RF-BUS-01 a RF-BUS-04, ADR-004). */
public interface ListingSearch {
  SearchPage search(SearchCriteria criteria);

  /** HU-14: determina si una tienda puede mostrarse públicamente. */
  boolean isActiveSeller(UUID sellerId);
}
