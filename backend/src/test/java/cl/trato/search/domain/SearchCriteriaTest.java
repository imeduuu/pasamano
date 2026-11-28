package cl.trato.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Reglas de filtros y orden de CU-04. */
class SearchCriteriaTest {
  @Test
  void defaults_page_size_and_order_based_on_text() {
    SearchCriteria textSearch =
        SearchCriteria.of("bicicleta", null, null, null, null, null, null, null, 0, null);
    SearchCriteria browse =
        SearchCriteria.of(null, null, null, null, null, null, null, null, 0, null);

    assertThat(textSearch.size()).isEqualTo(20);
    assertThat(textSearch.order()).isEqualTo(SearchOrder.RELEVANCE);
    assertThat(browse.order()).isEqualTo(SearchOrder.NEWEST);
    assertThat(textSearch.adjustments())
        .contains(SearchAdjustment.PAGE_SIZE_DEFAULTED, SearchAdjustment.ORDER_DEFAULTED);
  }

  @Test
  void resets_invalid_price_range_and_reports_adjustment() {
    SearchCriteria criteria =
        SearchCriteria.of(null, UUID.randomUUID(), 500L, 100L, null, null, null, null, 0, 20);

    assertThat(criteria.minimumPrice()).isNull();
    assertThat(criteria.maximumPrice()).isNull();
    assertThat(criteria.adjustments()).contains(SearchAdjustment.PRICE_RANGE_RESET);
  }

  @Test
  void resets_oversized_page_to_default_and_reports_adjustment() {
    SearchCriteria criteria =
        SearchCriteria.of(null, null, null, null, null, null, null, null, 2, 51);

    assertThat(criteria.size()).isEqualTo(20);
    assertThat(criteria.adjustments()).contains(SearchAdjustment.PAGE_SIZE_RESET);
  }

  @Test
  void preserves_valid_criteria_without_adjustments() {
    SearchCriteria criteria =
        SearchCriteria.of(
            "libro",
            UUID.randomUUID(),
            100L,
            500L,
            "USADO",
            "13",
            4.5,
            SearchOrder.PRICE_ASC,
            2,
            50);

    assertThat(criteria.minimumPrice()).isEqualTo(100L);
    assertThat(criteria.maximumPrice()).isEqualTo(500L);
    assertThat(criteria.page()).isEqualTo(2);
    assertThat(criteria.size()).isEqualTo(50);
    assertThat(criteria.adjustments()).isEmpty();
  }
}
