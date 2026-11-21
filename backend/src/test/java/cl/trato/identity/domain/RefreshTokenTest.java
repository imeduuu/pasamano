package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import cl.trato.identity.domain.exception.SessionExpiredException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Token de refresco de 7 días con rotación y detección de reutilización (RF-AUT-04). */
class RefreshTokenTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");
  static final UserId USER = UserId.newId();

  @Test
  void a_new_session_starts_a_new_family_and_lasts_seven_days() {
    RefreshToken token = RefreshToken.startFamily(USER, "hash-1", NOW);

    assertThat(token.familyId()).isNotNull();
    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    assertThat(token.isActive(NOW)).isTrue();
  }

  @Test
  void rotating_marks_the_current_token_as_used_and_issues_one_in_the_same_family() {
    RefreshToken current = RefreshToken.startFamily(USER, "hash-1", NOW);

    RefreshToken.Rotation rotation = current.rotate("hash-2", NOW.plusSeconds(60));

    assertThat(rotation.used().usedAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(rotation.used().isActive(NOW.plusSeconds(61))).isFalse();
    assertThat(rotation.next().familyId()).isEqualTo(current.familyId());
    assertThat(rotation.next().tokenHash()).isEqualTo("hash-2");
    assertThat(rotation.next().userId()).isEqualTo(USER);
    assertThat(rotation.next().expiresAt()).isEqualTo(NOW.plusSeconds(60).plus(Duration.ofDays(7)));
  }

  @Test
  void presenting_an_already_used_token_is_detected_as_reuse() {
    RefreshToken used = RefreshToken.startFamily(USER, "hash-1", NOW).rotate("hash-2", NOW).used();

    assertThatThrownBy(() -> used.rotate("hash-3", NOW.plusSeconds(5)))
        .isInstanceOf(RefreshTokenReuseException.class)
        .extracting("code")
        .isEqualTo("REFRESH_TOKEN_REUSED");
  }

  @Test
  void presenting_a_revoked_token_is_detected_as_reuse() {
    RefreshToken revoked = RefreshToken.startFamily(USER, "hash-1", NOW).revoke(NOW);

    assertThatThrownBy(() -> revoked.rotate("hash-2", NOW))
        .isInstanceOf(RefreshTokenReuseException.class);
  }

  @Test
  void an_expired_token_cannot_be_rotated() {
    RefreshToken token = RefreshToken.startFamily(USER, "hash-1", NOW);

    assertThatThrownBy(() -> token.rotate("hash-2", NOW.plus(Duration.ofDays(7))))
        .isInstanceOf(SessionExpiredException.class)
        .extracting("code")
        .isEqualTo("SESSION_EXPIRED");
  }

  @Test
  void revoking_is_idempotent_and_keeps_the_first_revocation_time() {
    RefreshToken revoked = RefreshToken.startFamily(USER, "hash-1", NOW).revoke(NOW);

    RefreshToken again = revoked.revoke(NOW.plusSeconds(30));

    assertThat(again.revokedAt()).isEqualTo(NOW);
    assertThat(again.isActive(NOW)).isFalse();
  }

  @Test
  void the_hash_is_required() {
    assertThatThrownBy(() -> RefreshToken.startFamily(USER, "", NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
