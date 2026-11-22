package cl.trato.catalog.infrastructure.in.web;

import cl.trato.catalog.application.CategoryView;
import cl.trato.catalog.application.ManageCategories;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administración del catálogo y registro de auditoría (CU-18, RF-CAT-02, RF-ADM-05). */
@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "Administración de categorías")
public class AdminCategoriesController {
  private final ManageCategories manageCategories;

  public AdminCategoriesController(ManageCategories manageCategories) {
    this.manageCategories = manageCategories;
  }

  record CreateRequest(
      @NotBlank @Size(max = 80) String name,
      @Min(0) int sortOrder,
      UUID parentId,
      @Size(max = 500) String reason) {}

  static class UpdateRequest {
    @Size(max = 80) private String name;

    @Min(0) private Integer sortOrder;

    private UUID parentId;
    private boolean parentSpecified;

    @Size(max = 500) private String reason;

    public String name() {
      return name;
    }

    public Integer sortOrder() {
      return sortOrder;
    }

    public UUID parentId() {
      return parentId;
    }

    public boolean parentSpecified() {
      return parentSpecified;
    }

    public String reason() {
      return reason;
    }

    @JsonSetter
    public void setName(String value) {
      name = value;
    }

    @JsonSetter
    public void setSortOrder(Integer value) {
      sortOrder = value;
    }

    @JsonSetter
    public void setParentId(UUID value) {
      parentSpecified = true;
      parentId = value;
    }

    @JsonSetter
    public void setReason(String value) {
      reason = value;
    }
  }

  record DeactivateRequest(@Size(max = 500) String reason) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  CategoryView create(@Valid @RequestBody CreateRequest request, Authentication authentication) {
    return manageCategories.create(
        UUID.fromString(authentication.getName()),
        request.name(),
        request.sortOrder(),
        request.parentId(),
        request.reason());
  }

  @PatchMapping("/{id}")
  CategoryView update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRequest request,
      Authentication authentication) {
    return manageCategories.update(
        UUID.fromString(authentication.getName()),
        id,
        request.name(),
        request.sortOrder(),
        request.parentId(),
        request.parentSpecified(),
        request.reason());
  }

  @PostMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivate(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) DeactivateRequest request,
      Authentication authentication) {
    manageCategories.deactivate(
        UUID.fromString(authentication.getName()), id, request == null ? null : request.reason());
  }
}
