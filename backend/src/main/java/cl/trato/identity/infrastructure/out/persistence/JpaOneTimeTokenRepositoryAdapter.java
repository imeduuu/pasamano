package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto {@link OneTimeTokenRepository}. */
@Component
class JpaOneTimeTokenRepositoryAdapter implements OneTimeTokenRepository {

  private final OneTimeTokenJpaRepository jpa;
  private final IdentityPersistenceMapper mapper;

  JpaOneTimeTokenRepositoryAdapter(
      OneTimeTokenJpaRepository jpa, IdentityPersistenceMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public void save(OneTimeToken token) {
    jpa.save(mapper.toEntity(token));
  }

  @Override
  public Optional<OneTimeToken> findByHash(String tokenHash, TokenPurpose purpose) {
    return jpa.findByTokenHashAndPurpose(tokenHash, purpose).map(mapper::toDomain);
  }

  @Override
  public void invalidateActive(UserId userId, TokenPurpose purpose, Instant now) {
    jpa.invalidateActive(userId.value(), purpose, now);
  }
}
