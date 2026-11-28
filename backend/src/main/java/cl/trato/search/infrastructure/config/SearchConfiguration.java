package cl.trato.search.infrastructure.config;

import cl.trato.search.application.SearchListings;
import cl.trato.search.application.port.ListingSearch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Search module wiring. */
@Configuration
public class SearchConfiguration {
  @Bean
  SearchListings searchListings(ListingSearch listingSearch) {
    return new SearchListings(listingSearch);
  }
}
