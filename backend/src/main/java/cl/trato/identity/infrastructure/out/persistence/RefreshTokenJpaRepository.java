package cl.trato.identity.infrastructure.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update RefreshTokenJpaEntity t set t.revokedAt = :now"
          + " where t.familyId = :familyId and t.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update RefreshTokenJpaEntity t set t.revokedAt = :now"
          + " where t.userId = :userId and t.revokedAt is null")
  int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
