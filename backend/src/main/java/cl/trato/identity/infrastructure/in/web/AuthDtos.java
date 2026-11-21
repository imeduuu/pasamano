package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.application.UserProfileView;
import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

/** DTOs de la API de identidad. Los mensajes de validación se muestran al usuario en español. */
final class AuthDtos {

  private AuthDtos() {}

  @Schema(description = "Datos del formulario de registro (CU-01)")
  record RegisterRequest(
      @NotBlank(message = "Ingresa tu nombre.") @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres.") @Schema(example = "Ana Pérez")
          String name,
      @NotBlank(message = "Ingresa tu correo.") @Email(message = "El correo electrónico no es válido.") @Size(max = 254, message = "El correo es demasiado largo.") @Schema(example = "ana@ejemplo.cl")
          String email,
      @NotBlank(message = "Ingresa una contraseña.") @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres.") @Schema(example = "caballo-bateria-grapa")
          String password,
      @AssertTrue(message = "Debes aceptar los términos y la política de privacidad.") @Schema(example = "true")
          boolean acceptTerms) {

    @Override
    public String toString() {
      return "RegisterRequest[name=" + name + "]";
    }
  }

  record VerifyRequest(@NotBlank(message = "Falta el código de verificación.") String token) {}

  record EmailRequest(
      @NotBlank(message = "Ingresa tu correo.") @Email(message = "El correo electrónico no es válido.") @Size(max = 254, message = "El correo es demasiado largo.") String email) {}

  record LoginRequest(
      @NotBlank(message = "Ingresa tu correo.") @Email(message = "El correo electrónico no es válido.") @Size(max = 254, message = "El correo es demasiado largo.") String email,
      @NotBlank(message = "Ingresa tu contraseña.") @Size(max = 128, message = "La contraseña es demasiado larga.") String password) {

    @Override
    public String toString() {
      return "LoginRequest[***]";
    }
  }

  record ResetPasswordRequest(
      @NotBlank(message = "Falta el código del enlace.") String token,
      @NotBlank(message = "Ingresa una contraseña.") @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres.") String password) {

    @Override
    public String toString() {
      return "ResetPasswordRequest[***]";
    }
  }

  record ChangePasswordRequest(
      @NotBlank(message = "Ingresa tu contraseña actual.") String currentPassword,
      @NotBlank(message = "Ingresa una contraseña nueva.") @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres.") String newPassword) {

    @Override
    public String toString() {
      return "ChangePasswordRequest[***]";
    }
  }

  @Schema(description = "Campos a modificar; los ausentes no cambian y un texto vacío los borra")
  record UpdateProfileRequest(
      @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres.") String name,
      @Size(max = 60, message = "La región puede tener como máximo 60 caracteres.") String region,
      @Size(max = 60, message = "La comuna puede tener como máximo 60 caracteres.") String commune,
      @Size(max = 500, message = "La URL de la foto es demasiado larga.") String photoUrl) {}

  @Schema(description = "Token de acceso. El de refresco viaja solo en la cookie HttpOnly")
  record TokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt) {}

  record MessageResponse(String message) {}

  record ProfileResponse(
      String id,
      String email,
      String name,
      String region,
      String commune,
      String photoUrl,
      UserStatus status,
      Set<Role> roles,
      Instant createdAt) {

    static ProfileResponse of(UserProfileView view) {
      return new ProfileResponse(
          view.id(),
          view.email(),
          view.name(),
          view.region(),
          view.commune(),
          view.photoUrl(),
          view.status(),
          view.roles(),
          view.createdAt());
    }
  }
}
