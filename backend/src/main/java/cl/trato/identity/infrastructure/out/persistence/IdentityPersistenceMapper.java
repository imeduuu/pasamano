package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.domain.Consent;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.UserId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapeo MapStruct entre los records del dominio y las entidades JPA de tokens y consentimiento. */
@Mapper
interface IdentityPersistenceMapper {

  RefreshTokenJpaEntity toEntity(RefreshToken token);

  /** {@code revoke} es una operación del dominio, no una propiedad. */
  @Mapping(target = "revoke", ignore = true)
  RefreshToken toDomain(RefreshTokenJpaEntity entity);

  OneTimeTokenJpaEntity toEntity(OneTimeToken token);

  /** {@code consume} es una operación del dominio, no una propiedad. */
  @Mapping(target = "consume", ignore = true)
  OneTimeToken toDomain(OneTimeTokenJpaEntity entity);

  ConsentJpaEntity toEntity(Consent consent);

  Consent toDomain(ConsentJpaEntity entity);

  default UUID map(UserId userId) {
    return userId == null ? null : userId.value();
  }

  default UserId map(UUID value) {
    return value == null ? null : new UserId(value);
  }
}
