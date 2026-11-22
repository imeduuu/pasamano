package cl.trato.catalog.infrastructure.config;

import cl.trato.catalog.application.GetCategories;
import cl.trato.catalog.application.ManageCategories;
import cl.trato.catalog.application.port.AuditLog;
import cl.trato.catalog.application.port.CategoryListingCount;
import cl.trato.catalog.application.port.CategoryRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado de los casos de uso del catálogo. */
@Configuration
public class CatalogConfiguration {
  @Bean
  GetCategories getCategories(CategoryRepository categories) {
    return new GetCategories(categories);
  }

  @Bean
  ManageCategories manageCategories(
      CategoryRepository categories,
      AuditLog auditLog,
      CategoryListingCount listingCount,
      Clock clock) {
    return new ManageCategories(categories, auditLog, listingCount, clock);
  }

  @Bean
  CategoryListingCount categoryListingCount() {
    return categoryId -> 0;
  }
}
