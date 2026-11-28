package cl.trato.search.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Criterios normalizados de búsqueda y ajustes de CU-04. */
public record SearchCriteria(
    String text,
    UUID categoryId,
    Long minimumPrice,
    Long maximumPrice,
    String condition,
    String region,
    Double minimumReputation,
    SearchOrder order,
    int page,
    int size,
    List<SearchAdjustment> adjustments) {

  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = 50;

  public SearchCriteria {
    text = text == null || text.isBlank() ? null : text.strip();
    region = region == null || region.isBlank() ? null : region.strip();
    List<SearchAdjustment> applied = new ArrayList<>();

    if (minimumPrice != null && maximumPrice != null && minimumPrice > maximumPrice) {
      minimumPrice = null;
      maximumPrice = null;
      applied.add(SearchAdjustment.PRICE_RANGE_RESET);
    }
    if (size == 0) {
      size = DEFAULT_PAGE_SIZE;
      applied.add(SearchAdjustment.PAGE_SIZE_DEFAULTED);
    } else if (size < 0 || size > MAX_PAGE_SIZE) {
      size = DEFAULT_PAGE_SIZE;
      applied.add(SearchAdjustment.PAGE_SIZE_RESET);
    }
    if (order == null) {
      order = text == null ? SearchOrder.NEWEST : SearchOrder.RELEVANCE;
      applied.add(SearchAdjustment.ORDER_DEFAULTED);
    }
    if (page < 0) {
      page = 0;
    }
    adjustments = List.copyOf(applied);
  }

  /** Normaliza los parámetros recibidos por CU-04. */
  public static SearchCriteria of(
      String text,
      UUID categoryId,
      Long minimumPrice,
      Long maximumPrice,
      String condition,
      String region,
      Double minimumReputation,
      SearchOrder order,
      int page,
      Integer size) {
    return new SearchCriteria(
        text,
        categoryId,
        minimumPrice,
        maximumPrice,
        condition,
        region,
        minimumReputation,
        order,
        page,
        size == null ? 0 : size,
        List.of());
  }
}
