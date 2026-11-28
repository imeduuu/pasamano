package cl.trato.search.application;

import cl.trato.search.application.port.ListingSearch;
import cl.trato.search.domain.SearchAdjustment;
import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchListing;
import cl.trato.search.domain.SearchPage;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.List;

/** Ejecuta CU-04 y devuelve resultados junto con los ajustes de criterios aplicados. */
public class SearchListings {
  public record Result(
      List<SearchListing> results, long total, List<SearchAdjustment> adjustments) {
    public Result {
      results = List.copyOf(results);
      adjustments = List.copyOf(adjustments);
    }
  }

  private final ListingSearch listingSearch;

  public SearchListings(ListingSearch listingSearch) {
    this.listingSearch = listingSearch;
  }

  public Result execute(SearchCriteria criteria) {
    SearchPage page = listingSearch.search(criteria);
    return new Result(page.results(), page.total(), criteria.adjustments());
  }

  /** HU-14: busca las publicaciones activas y recientes de una tienda visible. */
  public Result forSeller(java.util.UUID sellerId, int page, Integer size) {
    if (!listingSearch.isActiveSeller(sellerId)) {
      throw new ResourceNotFoundException("SELLER_NOT_FOUND", "No se encontró el vendedor.");
    }
    SearchCriteria criteria =
        SearchCriteria.of(null, null, null, null, null, null, null, null, page, size)
            .forSeller(sellerId);
    return execute(criteria);
  }
}
