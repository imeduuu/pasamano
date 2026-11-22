package cl.trato.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.exception.UserNotFoundException;
import cl.trato.shared.domain.exception.ConflictException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** CU-15: activa el perfil vendedor solo para cuentas activas y verificadas. */
class ActivateSellerProfileTest {

  private final IdentityFakes fakes = new IdentityFakes();

  @Test
  void activates_profile_and_records_terms_acceptance() {
    User user = fakes.activeUser("seller@ejemplo.cl");
    InMemorySellerProfiles profiles = new InMemorySellerProfiles();

    SellerProfile result =
        new ActivateSellerProfile(fakes.users, profiles, fakes.clock, "2026-11-20")
            .execute(user.id(), "Tienda Sur");

    assertThat(result.userId()).isEqualTo(user.id());
    assertThat(result.storeName()).isEqualTo("Tienda Sur");
    assertThat(result.termsVersion()).isEqualTo("2026-11-20");
    assertThat(result.termsAcceptedAt()).isEqualTo(fakes.clock.instant());
    assertThat(profiles.saved.values()).containsExactly(result);
  }

  @Test
  void rejects_missing_pending_and_suspended_users() {
    User pending = fakes.pendingUser("pending@ejemplo.cl");
    User suspended = fakes.activeUser("suspended@ejemplo.cl");
    suspended.suspend(fakes.clock.instant());
    fakes.users.save(suspended);
    InMemorySellerProfiles profiles = new InMemorySellerProfiles();
    ActivateSellerProfile useCase =
        new ActivateSellerProfile(fakes.users, profiles, fakes.clock, "2026-11-20");

    assertThatThrownBy(() -> useCase.execute(UserId.newId(), "Tienda"))
        .isInstanceOf(UserNotFoundException.class);
    assertThatThrownBy(() -> useCase.execute(pending.id(), "Tienda"))
        .extracting("code")
        .isEqualTo("EMAIL_NOT_VERIFIED");
    assertThatThrownBy(() -> useCase.execute(suspended.id(), "Tienda"))
        .extracting("code")
        .isEqualTo("ACCOUNT_SUSPENDED");
    assertThat(profiles.saved).isEmpty();
  }

  @Test
  void rejects_users_that_already_have_a_seller_profile() {
    User user = fakes.activeUser("seller@ejemplo.cl");
    InMemorySellerProfiles profiles = new InMemorySellerProfiles();
    profiles.save(SellerProfile.activate(user.id(), "Otra Tienda", "v1", fakes.clock.instant()));
    ActivateSellerProfile useCase =
        new ActivateSellerProfile(fakes.users, profiles, fakes.clock, "v1");

    assertThatThrownBy(() -> useCase.execute(user.id(), "Nueva Tienda"))
        .isInstanceOf(ConflictException.class);
  }

  private static final class InMemorySellerProfiles
      implements cl.trato.identity.application.port.SellerProfileRepository {
    final Map<UserId, SellerProfile> saved = new HashMap<>();

    @Override
    public Optional<SellerProfile> findByUserId(UserId id) {
      return Optional.ofNullable(saved.get(id));
    }

    @Override
    public boolean existsByNormalizedStoreName(String name) {
      return saved.values().stream()
          .anyMatch(profile -> profile.normalizedStoreName().equals(name));
    }

    @Override
    public SellerProfile save(SellerProfile profile) {
      saved.put(profile.userId(), profile);
      return profile;
    }
  }
}
