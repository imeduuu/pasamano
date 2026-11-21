package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.exception.InvalidTokenException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tokens de un solo uso: verificación de correo (24 h, RF-AUT-02) y recuperación (1 h, RF-AUT-06).
 */
class OneTimeTokenTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");
  static final UserId USER = UserId.newId();

  @Test
  void an_email_verification_token_lasts_24_hours() {
    OneTimeToken token = OneTimeToken.issue(USER, TokenPurpose.EMAIL_VERIFICATION, "hash", NOW);

    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    assertThat(token.usedAt()).isNull();
    assertThat(token.createdAt()).isEqualTo(NOW);
    assertThat(token.id()).isNotNull();
  }

  @Test
  void a_password_reset_token_lasts_one_hour() {
    OneTimeToken token = OneTimeToken.issue(USER, TokenPurpose.PASSWORD_RESET, "hash", NOW);

    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
  }

  @Test
  void consuming_a_valid_token_marks_it_as_used() {
    OneTimeToken token = OneTimeToken.issue(USER, TokenPurpose.PASSWORD_RESET, "hash", NOW);

    OneTimeToken used = token.consume(NOW.plusSeconds(60));

    assertThat(used.usedAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(used.id()).isEqualTo(token.id());
  }

  @Test
  void a_token_can_be_used_only_once() {
    OneTimeToken used =
        OneTimeToken.issue(USER, TokenPurpose.EMAIL_VERIFICATION, "hash", NOW).consume(NOW);

    assertThatThrownBy(() -> used.consume(NOW.plusSeconds(1)))
        .isInstanceOf(InvalidTokenException.class)
        .extracting("code")
        .isEqualTo("TOKEN_ALREADY_USED");
  }

  @Test
  void an_expired_token_cannot_be_used() {
    OneTimeToken token = OneTimeToken.issue(USER, TokenPurpose.PASSWORD_RESET, "hash", NOW);

    assertThatThrownBy(() -> token.consume(NOW.plus(Duration.ofHours(1))))
        .isInstanceOf(InvalidTokenException.class)
        .extracting("code")
        .isEqualTo("TOKEN_EXPIRED");
  }

  @Test
  void the_hash_is_required() {
    assertThatThrownBy(() -> OneTimeToken.issue(USER, TokenPurpose.PASSWORD_RESET, " ", NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
