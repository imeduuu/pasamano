package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.application.port.CategoryRepository;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.domain.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto de publicaciones. */
@Component
class JpaListingRepositoryAdapter implements ListingRepository {
  private final ListingJpaRepository jpa;
  private final CategoryRepository categories;
  private final ListingPersistenceMapper mapper = new ListingPersistenceMapper();

  JpaListingRepositoryAdapter(ListingJpaRepository jpa, CategoryRepository categories) {
    this.jpa = jpa;
    this.categories = categories;
  }

  @Override
  public Optional<Listing> findById(UUID id) {
    return jpa.findById(id)
        .map(entity ->
            mapper.toDomain(
                entity,
                categories
                    .findById(entity.getCategoryId())
                    .orElseThrow(() -> new IllegalStateException("Listing category is missing"))));
  }

  @Override
  public List<Listing> findBySeller(UserId seller, ListingStatus status, int offset, int limit) {
    var pageable = org.springframework.data.domain.PageRequest.of(offset / limit, limit);
    var rows =
        status == null
            ? jpa.findBySellerIdAndStatusNotOrderById(
                seller.value(), ListingStatus.ELIMINADA, pageable)
            : jpa.findBySellerIdAndStatusOrderById(seller.value(), status, pageable);
    return rows.stream()
        .map(entity ->
            mapper.toDomain(
                entity,
                categories
                    .findById(entity.getCategoryId())
                    .orElseThrow(() -> new IllegalStateException("Listing category is missing"))))
        .toList();
  }

  @Override
  public long countActiveBySeller(UserId seller) {
    return jpa.countBySellerIdAndStatus(seller.value(), ListingStatus.ACTIVA);
  }

  @Override
  public long countBySeller(UserId seller, ListingStatus status) {
    return status == null
        ? jpa.countBySellerIdAndStatusNot(seller.value(), ListingStatus.ELIMINADA)
        : jpa.countBySellerIdAndStatus(seller.value(), status);
  }

  @Override
  public Listing save(Listing listing) {
    ListingJpaEntity entity;
    if (listing.version() == null) {
      entity = mapper.toEntity(listing);
    } else {
      entity =
          jpa.findById(listing.id())
              .orElseThrow(() ->
                  new ObjectOptimisticLockingFailureException(
                      ListingJpaEntity.class, listing.id()));
      if (!listing.version().equals(entity.getVersion())) {
        throw new ObjectOptimisticLockingFailureException(ListingJpaEntity.class, listing.id());
      }
      mapper.updateEntity(listing, entity);
    }
    return mapper.toDomain(jpa.saveAndFlush(entity), listing.category());
  }
}
