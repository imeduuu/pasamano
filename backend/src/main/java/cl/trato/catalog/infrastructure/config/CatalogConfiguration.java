package cl.trato.catalog.infrastructure.config;

import cl.trato.catalog.application.GetCategories;
import cl.trato.catalog.application.GetListingAvailability;
import cl.trato.catalog.application.GetListings;
import cl.trato.catalog.application.ManageCategories;
import cl.trato.catalog.application.ManageListingImages;
import cl.trato.catalog.application.ManageListings;
import cl.trato.catalog.application.port.AuditLog;
import cl.trato.catalog.application.port.CategoryListingCount;
import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ImageProcessor;
import cl.trato.catalog.application.port.ImageStorage;
import cl.trato.catalog.application.port.ListingImages;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.infrastructure.out.image.ImageProcessorAdapter;
import cl.trato.catalog.infrastructure.out.storage.MinioImageStorageAdapter;
import cl.trato.geo.application.port.RegionRepository;
import cl.trato.identity.application.port.ActiveListingCount;
import cl.trato.identity.application.port.SellerProfileRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado de los casos de uso del catálogo. */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class CatalogConfiguration {
  @Bean
  ImageProcessor imageProcessor() {
    return new ImageProcessorAdapter();
  }

  @Bean(destroyMethod = "close")
  ImageStorage imageStorage(S3StorageProperties properties) {
    return new MinioImageStorageAdapter(
        properties.endpoint(),
        properties.bucket(),
        properties.accessKey(),
        properties.secretKey(),
        properties.region(),
        properties.urlExpiration());
  }

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

  @Bean
  ManageListings manageListings(
      ListingRepository listings,
      CategoryRepository categories,
      RegionRepository regions,
      SellerProfileRepository sellers) {
    return new ManageListings(listings, categories, regions, sellers);
  }

  @Bean
  GetListings getListings(
      ListingRepository listings,
      SellerProfileRepository sellers,
      ImageStorage images,
      ListingImages listingImages) {
    return new GetListings(listings, sellers, images, listingImages);
  }

  @Bean
  GetListingAvailability getListingAvailability(ListingRepository listings) {
    return new GetListingAvailability(listings);
  }

  @Bean
  ManageListingImages manageListingImages(
      ManageListings listings,
      ListingImages listingImages,
      ImageProcessor processor,
      ImageStorage storage) {
    return new ManageListingImages(listings, listingImages, processor, storage);
  }

  @Bean
  ActiveListingCount activeListingCount(ListingRepository listings) {
    return listings::countActiveBySeller;
  }
}
