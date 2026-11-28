package cl.trato.search.infrastructure.out.persistence;

import cl.trato.search.application.port.ListingSearch;
import cl.trato.search.domain.SearchCriteria;
import cl.trato.search.domain.SearchListing;
import cl.trato.search.domain.SearchOrder;
import cl.trato.search.domain.SearchPage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL implementation of listing search (RF-BUS-01 a RF-BUS-04, CU-04). */
@Repository
public class PostgresListingSearchAdapter implements ListingSearch {
  private static final String FROM =
      " FROM listings l JOIN users u ON u.id = l.seller_id "
          + "LEFT JOIN seller_profiles sp ON sp.user_id = u.id ";
  private final NamedParameterJdbcTemplate jdbc;

  public PostgresListingSearchAdapter(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public SearchPage search(SearchCriteria criteria) {
    Map<String, Object> values = new HashMap<>();
    StringBuilder where = new StringBuilder(" WHERE l.status = 'ACTIVA' AND u.status = 'ACTIVE'");
    if (criteria.text() != null) {
      values.put("text", criteria.text());
      where
          .append(" AND (l.search_vector @@ websearch_to_tsquery('spanish', :text) ")
          .append("OR immutable_unaccent(l.title) % immutable_unaccent(:text))");
    }
    if (criteria.categoryId() != null) {
      values.put("categoryId", criteria.categoryId());
      where
          .append(" AND l.category_id IN (SELECT id FROM categories WHERE id = :categoryId ")
          .append("UNION SELECT id FROM categories WHERE parent_id = :categoryId)");
    }
    if (criteria.minimumPrice() != null) {
      values.put("minimumPrice", criteria.minimumPrice());
      where.append(" AND l.price >= :minimumPrice");
    }
    if (criteria.maximumPrice() != null) {
      values.put("maximumPrice", criteria.maximumPrice());
      where.append(" AND l.price <= :maximumPrice");
    }
    if (criteria.condition() != null) {
      values.put("condition", criteria.condition());
      where.append(" AND l.condition = :condition");
    }
    if (criteria.region() != null) {
      values.put("region", criteria.region());
      where.append(" AND l.region = :region");
    }
    // Until reviews exist, the only known reputation is “Vendedor nuevo”.
    if (criteria.minimumReputation() != null) {
      where.append(" AND sp.user_id IS NOT NULL AND false");
    }

    long total =
        jdbc.queryForObject(
            "SELECT count(*)" + FROM + where, new MapSqlParameterSource(values), Long.class);
    values.put("limit", criteria.size());
    values.put("offset", (long) criteria.page() * criteria.size());
    String orderBy = orderBy(criteria.order(), criteria.text() != null);
    String score =
        criteria.text() == null
            ? "0::real"
            : "ts_rank(l.search_vector, websearch_to_tsquery('spanish', :text)) + "
                + "similarity(immutable_unaccent(l.title), immutable_unaccent(:text))";
    List<SearchListing> results =
        jdbc.query(
            "SELECT l.id, l.title, l.price, li.thumbnail_reference, l.region, l.commune, "
                + "coalesce(sp.store_name, 'Vendedor nuevo') AS store_name, "
                + "'Vendedor nuevo' AS reputation"
                + FROM
                + "LEFT JOIN listing_images li ON li.listing_id = l.id AND li.position = 0"
                + where
                + " ORDER BY "
                + (criteria.order() == SearchOrder.RELEVANCE && criteria.text() != null
                    ? score + " DESC, "
                    : "")
                + orderBy
                + " LIMIT :limit OFFSET :offset",
            new MapSqlParameterSource(values),
            (rs, row) ->
                new SearchListing(
                    rs.getObject("id", java.util.UUID.class),
                    rs.getString("title"),
                    rs.getLong("price"),
                    rs.getString("thumbnail_reference"),
                    rs.getString("region"),
                    rs.getString("commune"),
                    rs.getString("store_name"),
                    rs.getString("reputation")));
    return new SearchPage(results, total);
  }

  private static String orderBy(SearchOrder order, boolean hasText) {
    return switch (order) {
      case PRICE_ASC -> "l.price ASC, l.id ASC";
      case PRICE_DESC -> "l.price DESC, l.id ASC";
      case RELEVANCE ->
          hasText
              ? "l.published_at DESC NULLS LAST, l.id ASC"
              : "l.published_at DESC NULLS LAST, l.id ASC";
      case NEWEST -> "l.published_at DESC NULLS LAST, l.id ASC";
    };
  }
}
