package cl.trato.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** RF-USR-03 y RN-07: resumen público y reputación de vendedores sin calificaciones. */
class GetPublicSellerProfileTest {

  private static final Instant ACTIVATED_AT = Instant.parse("2026-11-20T12:00:00Z");

  @Test
  void returns_public_store_data_and_new_seller_reputation() {
    UserId id = UserId.newId();
    SellerProfile profile = SellerProfile.activate(id, "Tienda Sur", "v1", ACTIVATED_AT);
    GetPublicSellerProfile useCase =
        new GetPublicSellerProfile(repository(Optional.of(profile)), sellerId -> 0);

    PublicSellerProfileView view = useCase.execute(id);

    assertThat(view.storeName()).isEqualTo("Tienda Sur");
    assertThat(view.activatedAt()).isEqualTo(ACTIVATED_AT);
    assertThat(view.reputation()).isEqualTo("Vendedor nuevo");
    assertThat(view.completedSales()).isZero();
    assertThat(view.activeListings()).isZero();
  }

  @Test
  void throws_not_found_if_seller_profile_does_not_exist() {
    GetPublicSellerProfile useCase =
        new GetPublicSellerProfile(repository(Optional.empty()), sellerId -> 0);

    assertThatThrownBy(() -> useCase.execute(UserId.newId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("No se encontró el perfil vendedor.");
  }

  private static SellerProfileRepository repository(Optional<SellerProfile> profile) {
    return new SellerProfileRepository() {

      @Override
      public Optional<SellerProfile> findByUserId(UserId id) {
        return profile.filter(candidate -> candidate.userId().equals(id));
      }

      @Override
      public boolean existsByNormalizedStoreName(String normalizedStoreName) {
        return false;
      }

      @Override
      public SellerProfile save(SellerProfile sellerProfile) {
        return sellerProfile;
      }
    };
  }
}
