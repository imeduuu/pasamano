package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.application.ChangePassword;
import cl.trato.identity.application.ChangePasswordCommand;
import cl.trato.identity.application.GetProfile;
import cl.trato.identity.application.UpdateProfile;
import cl.trato.identity.application.UpdateProfileCommand;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.infrastructure.in.web.AuthDtos.ChangePasswordRequest;
import cl.trato.identity.infrastructure.in.web.AuthDtos.ProfileResponse;
import cl.trato.identity.infrastructure.in.web.AuthDtos.UpdateProfileRequest;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil propio (RF-USR-01, HU-06). El usuario siempre se toma del token de acceso, nunca de la URL
 * ni del cuerpo: nadie puede leer ni editar el perfil de otro (RF-AUT-09, RNF-SEG-08).
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Perfil", description = "Perfil del usuario autenticado")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
class MeController {

  private final GetProfile getProfile;
  private final UpdateProfile updateProfile;
  private final ChangePassword changePassword;
  private final RefreshTokenCookies cookies;

  MeController(
      GetProfile getProfile,
      UpdateProfile updateProfile,
      ChangePassword changePassword,
      RefreshTokenCookies cookies) {
    this.getProfile = getProfile;
    this.updateProfile = updateProfile;
    this.changePassword = changePassword;
    this.cookies = cookies;
  }

  @GetMapping
  @Operation(summary = "Ver mi perfil")
  ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
    return ProfileResponse.of(getProfile.execute(currentUser(jwt)));
  }

  @PatchMapping
  @Operation(summary = "Editar mi perfil: nombre visible, foto, región y comuna")
  ProfileResponse update(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
    return ProfileResponse.of(
        updateProfile.execute(
            new UpdateProfileCommand(
                currentUser(jwt),
                request.name(),
                request.region(),
                request.commune(),
                request.photoUrl())));
  }

  @PostMapping("/password")
  @Operation(
      summary = "Cambiar mi contraseña (RF-AUT-07)",
      description = "Pide la contraseña actual y cierra todas las sesiones abiertas.")
  ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
    changePassword.execute(
        new ChangePasswordCommand(
            currentUser(jwt), request.currentPassword(), request.newPassword()));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
        .build();
  }

  private static UserId currentUser(Jwt jwt) {
    return UserId.of(jwt.getSubject());
  }
}
