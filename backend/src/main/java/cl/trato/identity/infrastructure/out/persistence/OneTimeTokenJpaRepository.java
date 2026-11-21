package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.domain.TokenPurpose;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeTokenJpaEntity, UUID> {

  Optional<OneTimeTokenJpaEntity> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update OneTimeTokenJpaEntity t set t.usedAt = :now"
          + " where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
  int invalidateActive(
      @Param("userId") UUID userId,
      @Param("purpose") TokenPurpose purpose,
      @Param("now") Instant now);
}
