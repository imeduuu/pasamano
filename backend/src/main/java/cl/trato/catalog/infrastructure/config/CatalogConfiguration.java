package cl.trato.catalog.infrastructure.config;

import cl.trato.catalog.application.GetCategories;
import cl.trato.catalog.application.port.CategoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado de los casos de uso del catálogo. */
@Configuration
public class CatalogConfiguration {
  @Bean
  GetCategories getCategories(CategoryRepository categories) {
    return new GetCategories(categories);
  }
}
