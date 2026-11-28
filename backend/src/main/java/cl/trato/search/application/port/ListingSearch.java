package cl.trato.search.application.port;

import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchPage;

/** Puerto de búsqueda de publicaciones (RF-BUS-01 a RF-BUS-04, ADR-004). */
public interface ListingSearch {
  SearchPage search(SearchCriteria criteria);
}
