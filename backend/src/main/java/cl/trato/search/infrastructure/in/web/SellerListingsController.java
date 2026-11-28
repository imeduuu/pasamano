package cl.trato.search.infrastructure.in.web;

import cl.trato.search.application.SearchListings;
import cl.trato.search.domain.SearchAdjustment;
import cl.trato.search.domain.SearchListing;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public active listings for a seller (HU-14). */
@RestController
@RequestMapping("/api/v1/sellers/{id}/listings")
class SellerListingsController {
  record SellerListingsResponse(
      List<SearchListing> results,
      long total,
      int totalPages,
      List<SearchAdjustment> adjustments) {}

  private final SearchListings search;

  SellerListingsController(SearchListings search) {
    this.search = search;
  }

  @GetMapping
  SellerListingsResponse get(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size) {
    SearchListings.Result result = search.forSeller(id, page, size);
    int pageSize = size == null || size <= 0 || size > 50 ? 20 : size;
    int pages = (int) ((result.total() + pageSize - 1) / pageSize);
    return new SellerListingsResponse(
        result.results(), result.total(), pages, result.adjustments());
  }
}
