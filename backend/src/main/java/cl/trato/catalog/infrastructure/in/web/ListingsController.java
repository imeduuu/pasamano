package cl.trato.catalog.infrastructure.in.web;

import cl.trato.catalog.application.GetListings;
import cl.trato.catalog.application.ManageListings;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingCondition;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import cl.trato.shared.infrastructure.web.ProblemDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones del vendedor sobre sus publicaciones (HU-08 y HU-10). */
@RestController
@RequestMapping("/api/v1/listings")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
class ListingsController {
  static final class InvalidIfMatchException extends RuntimeException {}

  record ListingRequest(
      @NotBlank @Size(min = 10, max = 80) String title,
      @NotBlank @Size(max = 4000) String description,
      @Min(1) long price,
      @Min(0) int stock,
      @NotNull ListingCondition condition,
      @NotNull UUID categoryId,
      @NotBlank String region,
      @NotBlank String commune) {
    ManageListings.Command command() {
      return new ManageListings.Command(
          title, description, price, stock, condition, categoryId, region, commune);
    }
  }

  record ListingResponse(
      UUID id,
      String title,
      String description,
      long price,
      int stock,
      ListingCondition condition,
      UUID categoryId,
      String region,
      String commune,
      String status,
      long version) {
    static ListingResponse from(Listing listing) {
      return new ListingResponse(
          listing.id(),
          listing.title(),
          listing.description(),
          listing.price().amount(),
          listing.stockTotal(),
          listing.condition(),
          listing.category().id(),
          listing.region(),
          listing.commune(),
          listing.status().name(),
          listing.version());
    }
  }

  private final ManageListings listings;
  private final GetListings queries;

  ListingsController(ManageListings listings, GetListings queries) {
    this.listings = listings;
    this.queries = queries;
  }

  @GetMapping("/{id}")
  ResponseEntity<GetListings.ListingView> get(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    GetListings.ListingView listing = queries.get(id, jwt == null ? null : user(jwt));
    return ResponseEntity.ok().eTag(etag(listing.version())).body(listing);
  }

  @ExceptionHandler(InvalidIfMatchException.class)
  ResponseEntity<ProblemDetail> invalidIfMatch() {
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .body(
            ProblemDetails.of(
                HttpStatus.PRECONDITION_FAILED,
                "INVALID_IF_MATCH",
                "La cabecera If-Match debe contener un ETag válido."));
  }

  @PostMapping
  ResponseEntity<ListingResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ListingRequest request) {
    Listing saved = listings.create(user(jwt), request.command());
    return ResponseEntity.created(URI.create("/api/v1/listings/" + saved.id()))
        .eTag(etag(saved.version()))
        .body(ListingResponse.from(saved));
  }

  @PatchMapping("/{id}")
  ResponseEntity<ListingResponse> update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @Valid @RequestBody ListingRequest request) {
    long version = parseVersion(ifMatch);
    Listing saved = listings.update(user(jwt), id, version, request.command());
    return ResponseEntity.ok().eTag(etag(saved.version())).body(ListingResponse.from(saved));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    listings.delete(user(jwt), id, parseVersion(ifMatch));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/{action:publish|pause|activate}")
  ResponseEntity<ListingResponse> transition(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @PathVariable String action,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    Listing saved = listings.transition(user(jwt), id, parseVersion(ifMatch), action);
    return ResponseEntity.ok().eTag(etag(saved.version())).body(ListingResponse.from(saved));
  }

  private static UserId user(Jwt jwt) {
    return UserId.of(jwt.getSubject());
  }

  private static String etag(Long version) {
    return "\"" + version + "\"";
  }

  private static long parseVersion(String header) {
    if (header == null || !header.matches("\\\"\\d+\\\"")) {
      throw new InvalidIfMatchException();
    }
    try {
      return Long.parseLong(header.substring(1, header.length() - 1));
    } catch (NumberFormatException exception) {
      throw new InvalidIfMatchException();
    }
  }
}
