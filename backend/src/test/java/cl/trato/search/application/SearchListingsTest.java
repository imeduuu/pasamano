package cl.trato.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.trato.search.application.port.ListingSearch;
import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchListing;
import cl.trato.search.domain.SearchPage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Resultado de la búsqueda solicitado en CU-04. */
class SearchListingsTest {
  @Test
  void returns_results_total_and_criteria_adjustments() {
    ListingSearch listingSearch = mock(ListingSearch.class);
    SearchCriteria criteria =
        SearchCriteria.of(null, null, 500L, 100L, null, null, null, null, 0, 51);
    SearchPage page =
        new SearchPage(
            List.of(
                new SearchListing(
                    UUID.randomUUID(),
                    "Libro",
                    1200,
                    null,
                    "13",
                    "13101",
                    "Tienda",
                    "Vendedor nuevo")),
            12);
    when(listingSearch.search(criteria)).thenReturn(page);

    SearchListings.Result result = new SearchListings(listingSearch).execute(criteria);

    assertThat(result.results()).isEqualTo(page.results());
    assertThat(result.total()).isEqualTo(12);
    assertThat(result.adjustments()).containsExactlyElementsOf(criteria.adjustments());
    verify(listingSearch).search(criteria);
  }
}
