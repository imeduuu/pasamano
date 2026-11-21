package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto {@link RefreshTokenRepository}. */
@Component
class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpa;
  private final IdentityPersistenceMapper mapper;

  JpaRefreshTokenRepositoryAdapter(
      RefreshTokenJpaRepository jpa, IdentityPersistenceMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public void save(RefreshToken token) {
    jpa.save(mapper.toEntity(token));
  }

  @Override
  public Optional<RefreshToken> findByHash(String tokenHash) {
    return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
  }

  @Override
  public void revokeFamily(UUID familyId, Instant now) {
    jpa.revokeFamily(familyId, now);
  }

  @Override
  public void revokeAllForUser(UserId userId, Instant now) {
    jpa.revokeAllForUser(userId.value(), now);
  }
}
