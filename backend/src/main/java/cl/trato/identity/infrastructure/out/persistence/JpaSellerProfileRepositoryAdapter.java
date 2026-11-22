package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Adaptador JPA del perfil vendedor; PostgreSQL garantiza unicidad también ante carreras. */
@Component
class JpaSellerProfileRepositoryAdapter implements SellerProfileRepository {

  private final SellerProfileJpaRepository jpa;

  JpaSellerProfileRepositoryAdapter(SellerProfileJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public java.util.Optional<SellerProfile> findByUserId(UserId id) {
    return jpa.findByUserId(id.value()).map(JpaSellerProfileRepositoryAdapter::toDomain);
  }

  @Override
  public boolean existsByNormalizedStoreName(String name) {
    return jpa.existsByNormalizedStoreName(name);
  }

  @Override
  public SellerProfile save(SellerProfile profile) {
    try {
      return toDomain(jpa.saveAndFlush(toEntity(profile)));
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException(
          "SELLER_PROFILE_CONFLICT",
          "El perfil vendedor ya existe o el nombre de tienda está en uso.");
    }
  }

  private static SellerProfile toDomain(SellerProfileJpaEntity entity) {
    return new SellerProfile(
        new UserId(entity.getUserId()),
        entity.getStoreName(),
        entity.getNormalizedStoreName(),
        entity.getTermsVersion(),
        entity.getTermsAcceptedAt());
  }

  private static SellerProfileJpaEntity toEntity(SellerProfile profile) {
    SellerProfileJpaEntity entity = new SellerProfileJpaEntity();
    entity.setUserId(profile.userId().value());
    entity.setStoreName(profile.storeName());
    entity.setNormalizedStoreName(profile.normalizedStoreName());
    entity.setTermsVersion(profile.termsVersion());
    entity.setTermsAcceptedAt(profile.termsAcceptedAt());
    return entity;
  }
}
