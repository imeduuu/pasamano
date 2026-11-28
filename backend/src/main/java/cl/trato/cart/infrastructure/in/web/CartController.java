package cl.trato.cart.infrastructure.in.web;

import cl.trato.cart.application.CartService;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API del carrito individual por comprador (HU-15/CU-05). */
@RestController
@RequestMapping("/api/v1/cart")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
class CartController {
  record QuantityRequest(@Min(1) int quantity) {}

  private final CartService carts;

  CartController(CartService carts) {
    this.carts = carts;
  }

  @GetMapping
  CartService.CartView get(@AuthenticationPrincipal Jwt jwt) {
    return carts.get(UserId.of(jwt.getSubject()));
  }

  @PutMapping("/items/{listingId}")
  ResponseEntity<CartService.CartView> put(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID listingId,
      @Valid @RequestBody QuantityRequest request) {
    carts.put(UserId.of(jwt.getSubject()), listingId, request.quantity());
    return ResponseEntity.ok(carts.get(UserId.of(jwt.getSubject())));
  }

  @DeleteMapping("/items/{listingId}")
  ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
    carts.delete(UserId.of(jwt.getSubject()), listingId);
    return ResponseEntity.noContent().build();
  }
}
