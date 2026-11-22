package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.domain.Category;
import cl.trato.catalog.domain.Listing;
import cl.trato.shared.domain.Money;
import java.util.UUID;

/** Traduce el agregado de dominio a filas JPA y viceversa. */
class ListingPersistenceMapper {

  ListingJpaEntity toEntity(Listing listing) {
    ListingJpaEntity entity = new ListingJpaEntity();
    updateEntity(listing, entity);
    return entity;
  }

  void updateEntity(Listing listing, ListingJpaEntity entity) {
    entity.setId(listing.id());
    entity.setTitle(listing.title());
    entity.setDescription(listing.description());
    entity.setPrice(listing.price().amount());
    entity.setStockTotal(listing.stockTotal());
    entity.setStockReserved(listing.stockReserved());
    entity.setCondition(listing.condition());
    entity.setCategoryId(listing.category().id());
    entity.setRegion(listing.region());
    entity.setCommune(listing.commune());
    entity.setSellerId(listing.seller().value());
    entity.setStatus(listing.status());
    var images = entity.getImages();
    for (int index = 0; index < listing.images().size(); index++) {
      ListingImageJpaEntity image;
      if (index < images.size()) {
        image = images.get(index);
      } else {
        image = new ListingImageJpaEntity();
        image.setId(UUID.randomUUID());
        image.setListing(entity);
        images.add(image);
      }
      image.setPosition(index);
      image.setImageReference(listing.images().get(index));
    }
    while (images.size() > listing.images().size()) {
      images.remove(images.size() - 1);
    }
  }

  Listing toDomain(ListingJpaEntity entity, Category category) {
    return Listing.rehydrate(
        entity.getId(),
        entity.getTitle(),
        entity.getDescription(),
        Money.price(entity.getPrice()),
        entity.getStockTotal(),
        entity.getStockReserved(),
        entity.getCondition(),
        category,
        entity.getRegion(),
        entity.getCommune(),
        new cl.trato.identity.domain.UserId(entity.getSellerId()),
        entity.getStatus(),
        entity.getImages().stream().map(ListingImageJpaEntity::getImageReference).toList(),
        entity.getVersion());
  }
}
