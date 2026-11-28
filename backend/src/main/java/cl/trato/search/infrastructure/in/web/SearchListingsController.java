package cl.trato.search.infrastructure.in.web;

import cl.trato.catalog.domain.ListingCondition;
import cl.trato.search.application.SearchListings;
import cl.trato.search.domain.SearchAdjustment;
import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchListing;
import cl.trato.search.domain.SearchOrder;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public listing search endpoint (RF-BUS-01 a RF-BUS-04, CU-04). */
@RestController
@RequestMapping("/api/v1/listings")
class SearchListingsController {
  record SearchResponse(
      java.util.List<SearchListing> results,
      long total,
      int totalPages,
      java.util.List<SearchAdjustment> adjustments) {}

  private final SearchListings search;

  SearchListingsController(SearchListings search) {
    this.search = search;
  }

  @GetMapping
  SearchResponse search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID category,
      @RequestParam(required = false) Long minPrice,
      @RequestParam(required = false) Long maxPrice,
      @RequestParam(required = false) ListingCondition condition,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) Double minRating,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size) {
    if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0) {
      throw badRequest("minPrice y maxPrice deben ser mayores o iguales a cero.");
    }
    if (minRating != null && (minRating < 0 || minRating > 5)) {
      throw badRequest("minRating debe estar entre 0 y 5.");
    }
    SearchOrder order = parseOrder(sort);
    SearchCriteria criteria =
        SearchCriteria.of(
            q,
            category,
            minPrice,
            maxPrice,
            condition == null ? null : condition.name(),
            region,
            minRating,
            order,
            page,
            size);
    SearchListings.Result result = search.execute(criteria);
    int pages = (int) ((result.total() + criteria.size() - 1) / criteria.size());
    return new SearchResponse(result.results(), result.total(), pages, result.adjustments());
  }

  private static SearchOrder parseOrder(String sort) {
    if (sort == null || sort.isBlank()) {
      return null;
    }
    return switch (sort) {
      case "relevance" -> SearchOrder.RELEVANCE;
      case "price_asc" -> SearchOrder.PRICE_ASC;
      case "price_desc" -> SearchOrder.PRICE_DESC;
      case "recent" -> SearchOrder.NEWEST;
      default -> throw badRequest("sort debe ser relevance, price_asc, price_desc o recent.");
    };
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
