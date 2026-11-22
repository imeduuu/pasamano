package cl.trato.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** HU-07 / CU-15 / RF-USR-02: datos y aceptación de términos del perfil vendedor. */
class SellerProfileTest {

  @Test
  void accepts_store_names_between_three_and_forty_characters_and_normalizes_lookup_key() {
    SellerProfile profile =
        SellerProfile.activate(UserId.newId(), "  Tienda Norte  ", "2026-11-20", Instant.EPOCH);

    assertThat(profile.storeName()).isEqualTo("Tienda Norte");
    assertThat(profile.normalizedStoreName()).isEqualTo("tienda norte");
    assertThat(profile.termsVersion()).isEqualTo("2026-11-20");
    assertThat(profile.termsAcceptedAt()).isEqualTo(Instant.EPOCH);
  }

  @Test
  void rejects_blank_short_or_long_names_and_missing_terms_acceptance() {
    for (String name : new String[] {"", "ab", "a".repeat(41)}) {
      assertThatThrownBy(() -> SellerProfile.activate(UserId.newId(), name, "v1", Instant.EPOCH))
          .extracting("code")
          .isEqualTo("INVALID_STORE_NAME");
    }
    assertThatThrownBy(() -> SellerProfile.activate(UserId.newId(), "Tienda", " ", Instant.EPOCH))
        .extracting("code")
        .isEqualTo("TERMS_ACCEPTANCE_REQUIRED");
  }
}
