package cl.trato.catalog.infrastructure.in.web;

import cl.trato.catalog.application.ManageListingImages;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import cl.trato.shared.infrastructure.web.ProblemDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Endpoints para imágenes de publicaciones (HU-09, RF-PUB-02). */
@RestController
@RequestMapping("/api/v1/listings/{id}/images")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
class ListingImagesController {
  record OrderRequest(List<UUID> imageIds) {}

  record ImageResponse(UUID id) {}

  private final ManageListingImages images;

  ListingImagesController(ManageListingImages images) {
    this.images = images;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> invalidImage(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(
            ProblemDetails.of(
                HttpStatus.BAD_REQUEST, "INVALID_LISTING_IMAGE", exception.getMessage()));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ImageResponse> add(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestPart("image") MultipartFile file)
      throws java.io.IOException {
    UUID imageId =
        images.add(UserId.of(jwt.getSubject()), id, file.getOriginalFilename(), file.getBytes());
    return ResponseEntity.status(201).body(new ImageResponse(imageId));
  }

  @DeleteMapping("/{imageId}")
  ResponseEntity<Void> remove(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID imageId) {
    images.remove(UserId.of(jwt.getSubject()), id, imageId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/order")
  ResponseEntity<Void> reorder(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody OrderRequest request) {
    images.reorder(UserId.of(jwt.getSubject()), id, request.imageIds());
    return ResponseEntity.noContent().build();
  }
}
