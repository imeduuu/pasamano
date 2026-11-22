package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.application.GetPublicSellerProfile;
import cl.trato.identity.application.PublicSellerProfileView;
import cl.trato.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública de tiendas (RF-USR-03). */
@RestController
@RequestMapping("/api/v1/sellers")
@Tag(name = "Vendedores", description = "Información pública de las tiendas")
class SellersController {

  private final GetPublicSellerProfile getPublicSellerProfile;

  SellersController(GetPublicSellerProfile getPublicSellerProfile) {
    this.getPublicSellerProfile = getPublicSellerProfile;
  }

  @GetMapping("/{id}")
  @Operation(summary = "Ver una tienda (RF-USR-03)")
  PublicSellerProfileResponse get(@PathVariable String id) {
    return PublicSellerProfileResponse.of(getPublicSellerProfile.execute(UserId.of(id)));
  }

  record PublicSellerProfileResponse(
      String storeName,
      Instant activatedAt,
      String reputation,
      long completedSales,
      long activeListings) {

    static PublicSellerProfileResponse of(PublicSellerProfileView view) {
      return new PublicSellerProfileResponse(
          view.storeName(),
          view.activatedAt(),
          view.reputation(),
          view.completedSales(),
          view.activeListings());
    }
  }
}
