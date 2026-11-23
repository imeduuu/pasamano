package cl.trato.catalog.infrastructure.in.web;

import cl.trato.catalog.application.GetListings;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta paginada de publicaciones propias (RF-BUS-05). */
@RestController
@RequestMapping("/api/v1/me/listings")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
class MyListingsController {
  private final GetListings queries;

  MyListingsController(GetListings queries) {
    this.queries = queries;
  }

  @GetMapping
  GetListings.ListingPage get(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) ListingStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return queries.mine(
        UserId.of(jwt.getSubject()), status, Math.max(0, page), Math.max(1, Math.min(size, 50)));
  }
}
