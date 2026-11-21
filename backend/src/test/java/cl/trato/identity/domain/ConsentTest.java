package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Consentimiento informado: fecha y versión de términos y política de privacidad (RNF-LEG-01). */
class ConsentTest {

  static final Instant NOW = Instant.parse("2026-12-07T12:00:00Z");

  @Test
  void records_the_versions_accepted_and_when() {
    UserId user = UserId.newId();

    Consent consent = Consent.accept(user, new LegalVersions("2026-11-20", "2026-11-21"), NOW);

    assertThat(consent.id()).isNotNull();
    assertThat(consent.userId()).isEqualTo(user);
    assertThat(consent.termsVersion()).isEqualTo("2026-11-20");
    assertThat(consent.privacyVersion()).isEqualTo("2026-11-21");
    assertThat(consent.acceptedAt()).isEqualTo(NOW);
  }

  @Test
  void versions_are_required() {
    assertThatThrownBy(() -> new LegalVersions(" ", "v1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LegalVersions("v1", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void terms_must_be_accepted_explicitly() {
    assertThatThrownBy(() -> Consent.requireAccepted(false))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("TERMS_NOT_ACCEPTED");
  }
}
