package cl.trato.catalog.infrastructure.config;

import cl.trato.catalog.application.port.ImageStorage;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Datos de desarrollo exclusivos del perfil local (RF-CAT-01, RF-PUB-01 y RF-PUB-02). */
@Configuration
@Profile("local")
class LocalSeedData {

  private static final List<CategorySeed> CATEGORIES =
      List.of(
          new CategorySeed("Tecnología", "tecnologia", 0, null),
          new CategorySeed("Celulares", "celulares", 0, "tecnologia"),
          new CategorySeed("Computación", "computacion", 1, "tecnologia"),
          new CategorySeed("Hogar", "hogar", 1, null),
          new CategorySeed("Muebles", "muebles", 2, "hogar"),
          new CategorySeed("Electrodomésticos", "electrodomesticos", 3, "hogar"),
          new CategorySeed("Deportes", "deportes", 2, null),
          new CategorySeed("Bicicletas", "bicicletas", 4, "deportes"),
          new CategorySeed("Moda", "moda", 3, null),
          new CategorySeed("Ropa", "ropa", 5, "moda"));

  record PlaceholderImages(
      String originalKey, byte[] original, String thumbnailKey, byte[] thumbnail) {}

  static PlaceholderImages placeholderImages() {
    String svg =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"640\" height=\"480\" viewBox=\"0 0 640 480\">"
            + "<rect width=\"640\" height=\"480\" fill=\"#e8eef5\"/>"
            + "<text x=\"320\" y=\"245\" text-anchor=\"middle\" font-family=\"sans-serif\" "
            + "font-size=\"28\" fill=\"#334155\">Publicación de prueba</text></svg>";
    String thumbnail = svg.replace("width=\"640\" height=\"480\"", "width=\"320\" height=\"240\"");
    return new PlaceholderImages(
        "seed/placeholder.svg",
        svg.getBytes(StandardCharsets.UTF_8),
        "seed/placeholder-thumb.svg",
        thumbnail.getBytes(StandardCharsets.UTF_8));
  }

  @Bean
  ApplicationRunner seedLocalData(
      JdbcTemplate jdbc,
      ImageStorage imageStorage,
      @Value("${trato.seed.listings:0}") int listingCount,
      @Value("${trato.identity.legal.terms-version:2026-11-20}") String termsVersion) {
    return args -> seed(jdbc, imageStorage, listingCount, termsVersion);
  }

  @Transactional
  void seed(JdbcTemplate jdbc, ImageStorage imageStorage, int listingCount, String termsVersion) {
    if (listingCount < 0) {
      throw new IllegalArgumentException("trato.seed.listings no puede ser negativo");
    }
    seedCategories(jdbc);
    if (listingCount == 0) {
      return;
    }

    PlaceholderImages images = placeholderImages();
    imageStorage.store(images.originalKey(), images.original(), "image/svg+xml");
    imageStorage.store(images.thumbnailKey(), images.thumbnail(), "image/svg+xml");

    Faker faker = new Faker(new Locale("es"));
    List<CategoryChoice> categories =
        jdbc.query(
            "select id, name from categories where parent_id is not null and active order by sort_order",
            (rs, row) -> new CategoryChoice(rs.getObject("id", UUID.class), rs.getString("name")));
    List<LocationChoice> locations =
        jdbc.query(
            "select r.name region, c.name commune from regions r join communes c on c.region_code = r.code",
            (rs, row) -> new LocationChoice(rs.getString("region"), rs.getString("commune")));
    if (categories.isEmpty() || locations.isEmpty()) {
      throw new IllegalStateException(
          "No existen categorías u ubicaciones para generar publicaciones");
    }

    Instant now = Instant.now();
    List<UUID> sellers = seedSellers(jdbc, faker, now, termsVersion);
    List<Object[]> listings = new ArrayList<>(listingCount);
    List<Object[]> listingImages = new ArrayList<>(listingCount);
    for (int index = 0; index < listingCount; index++) {
      UUID listingId = UUID.randomUUID();
      CategoryChoice category = categories.get(faker.number().numberBetween(0, categories.size()));
      LocationChoice location = locations.get(faker.number().numberBetween(0, locations.size()));
      String product = faker.commerce().productName();
      String title = (product + " en " + category.name());
      if (title.length() > 80) {
        title = title.substring(0, 80);
      }
      listings.add(
          new Object[] {
            listingId,
            title,
            faker.lorem().sentence(12),
            faker.number().numberBetween(1000, 99999999),
            faker.number().numberBetween(1, 15),
            0,
            faker.options().option("NUEVO", "USADO"),
            category.id(),
            location.region(),
            location.commune(),
            sellers.get(faker.number().numberBetween(0, sellers.size())),
            "ACTIVA"
          });
      listingImages.add(
          new Object[] {
            UUID.randomUUID(), listingId, 0, images.originalKey(), images.thumbnailKey()
          });
    }
    jdbc.batchUpdate(
        "insert into listings (id,title,description,price,stock_total,stock_reserved,condition,"
            + "category_id,region,commune,seller_id,status) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?)",
        listings);
    jdbc.batchUpdate(
        "insert into listing_images (id,listing_id,position,image_reference,thumbnail_reference) values (?,?,?,?,?)",
        listingImages);
  }

  private void seedCategories(JdbcTemplate jdbc) {
    Map<String, UUID> ids = new java.util.HashMap<>();
    CATEGORIES.forEach(category -> ids.put(category.slug(), categoryId(category.slug())));
    for (CategorySeed category : CATEGORIES) {
      jdbc.update(
          "insert into categories (id,name,slug,sort_order,active,parent_id) "
              + "values (?,?,?,?,true,?) on conflict (slug) do nothing",
          ids.get(category.slug()),
          category.name(),
          category.slug(),
          category.sortOrder(),
          category.parentSlug() == null ? null : ids.get(category.parentSlug()));
    }
  }

  private List<UUID> seedSellers(JdbcTemplate jdbc, Faker faker, Instant now, String termsVersion) {
    List<UUID> sellers = new ArrayList<>();
    for (int index = 0; index < 20; index++) {
      UUID id =
          UUID.nameUUIDFromBytes(
              ("trato-local-seed-seller-" + index).getBytes(StandardCharsets.UTF_8));
      String name = faker.name().fullName();
      String store = "Tienda Trato " + (index + 1);
      jdbc.update(
          "insert into users (id,email,password_hash,name,status,"
              + "failed_login_attempts,created_at,updated_at) "
              + "values (?,?,?,?,'ACTIVE',0,?,?) on conflict (id) do nothing",
          id,
          "seed-vendedor-" + index + "@trato.local",
          "!seed-no-login",
          name.length() > 60 ? name.substring(0, 60) : name,
          Timestamp.from(now),
          Timestamp.from(now));
      jdbc.update(
          "insert into user_roles (user_id,role) values (?,'USER') on conflict do nothing", id);
      jdbc.update(
          "insert into seller_profiles (user_id,store_name,normalized_store_name,terms_version,terms_accepted_at) "
              + "values (?,?,?,?,?) on conflict (user_id) do nothing",
          id,
          store,
          store.toLowerCase(Locale.ROOT),
          termsVersion,
          Timestamp.from(now));
      sellers.add(id);
    }
    return sellers;
  }

  private static UUID categoryId(String slug) {
    return UUID.nameUUIDFromBytes(
        ("trato-local-category-" + slug).getBytes(StandardCharsets.UTF_8));
  }

  private record CategorySeed(String name, String slug, int sortOrder, String parentSlug) {}

  private record CategoryChoice(UUID id, String name) {}

  private record LocationChoice(String region, String commune) {}
}
