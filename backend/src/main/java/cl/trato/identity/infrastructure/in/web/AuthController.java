package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.application.AuthSession;
import cl.trato.identity.application.Login;
import cl.trato.identity.application.LoginCommand;
import cl.trato.identity.application.Logout;
import cl.trato.identity.application.RefreshSession;
import cl.trato.identity.application.RegisterUser;
import cl.trato.identity.application.RegisterUserCommand;
import cl.trato.identity.application.RequestPasswordReset;
import cl.trato.identity.application.ResendVerification;
import cl.trato.identity.application.ResetPassword;
import cl.trato.identity.application.ResetPasswordCommand;
import cl.trato.identity.application.VerifyEmail;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.exception.RefreshTokenReuseException;
import cl.trato.identity.domain.exception.SessionExpiredException;
import cl.trato.identity.infrastructure.in.web.AuthDtos.EmailRequest;
import cl.trato.identity.infrastructure.in.web.AuthDtos.LoginRequest;
import cl.trato.identity.infrastructure.in.web.AuthDtos.MessageResponse;
import cl.trato.identity.infrastructure.in.web.AuthDtos.RegisterRequest;
import cl.trato.identity.infrastructure.in.web.AuthDtos.ResetPasswordRequest;
import cl.trato.identity.infrastructure.in.web.AuthDtos.TokenResponse;
import cl.trato.identity.infrastructure.in.web.AuthDtos.VerifyRequest;
import cl.trato.shared.infrastructure.config.OpenApiConfiguration;
import cl.trato.shared.infrastructure.web.ProblemDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticación: registro, verificación, sesión y recuperación de contraseña (HU-01 a HU-05). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y recuperación")
class AuthController {

  static final String VERIFICATION_SENT =
      "Si los datos son válidos, te enviamos un correo para verificar tu cuenta.";
  static final String RESET_SENT =
      "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.";

  private final RegisterUser registerUser;
  private final VerifyEmail verifyEmail;
  private final ResendVerification resendVerification;
  private final Login login;
  private final RefreshSession refreshSession;
  private final Logout logout;
  private final RequestPasswordReset requestPasswordReset;
  private final ResetPassword resetPassword;
  private final RefreshTokenCookies cookies;
  private final LoginRateLimiter loginRateLimiter;
  private final Clock clock;

  AuthController(
      RegisterUser registerUser,
      VerifyEmail verifyEmail,
      ResendVerification resendVerification,
      Login login,
      RefreshSession refreshSession,
      Logout logout,
      RequestPasswordReset requestPasswordReset,
      ResetPassword resetPassword,
      RefreshTokenCookies cookies,
      LoginRateLimiter loginRateLimiter,
      Clock clock) {
    this.registerUser = registerUser;
    this.verifyEmail = verifyEmail;
    this.resendVerification = resendVerification;
    this.login = login;
    this.refreshSession = refreshSession;
    this.logout = logout;
    this.requestPasswordReset = requestPasswordReset;
    this.resetPassword = resetPassword;
    this.cookies = cookies;
    this.loginRateLimiter = loginRateLimiter;
    this.clock = clock;
  }

  @PostMapping("/register")
  @Operation(
      summary = "Registrar usuario (CU-01)",
      description =
          "Responde lo mismo si el correo ya existe, para no revelar qué correos están"
              + " registrados; en ese caso se avisa al titular por correo.")
  ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
    registerUser.execute(
        new RegisterUserCommand(
            request.name(), request.email(), request.password(), request.acceptTerms()));
    return ResponseEntity.accepted().body(new MessageResponse(VERIFICATION_SENT));
  }

  @PostMapping("/verify")
  @Operation(summary = "Verificar el correo con el token del enlace")
  MessageResponse verify(@Valid @RequestBody VerifyRequest request) {
    verifyEmail.execute(request.token());
    return new MessageResponse("Tu cuenta quedó verificada. Ya puedes iniciar sesión.");
  }

  @PostMapping("/verify/resend")
  @Operation(summary = "Reenviar el correo de verificación (CU-01 8a, CU-02 2c)")
  ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
    resendVerification.execute(request.email());
    return ResponseEntity.accepted().body(new MessageResponse(VERIFICATION_SENT));
  }

  @PostMapping("/login")
  @Operation(
      summary = "Iniciar sesión (CU-02)",
      description =
          "Devuelve el token de acceso (15 min) y deja el de refresco (7 días) en una cookie"
              + " HttpOnly. Límite: 5 intentos por minuto por IP y correo.")
  ResponseEntity<TokenResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    loginRateLimiter.check(http.getRemoteAddr(), request.email());
    AuthSession session = login.execute(new LoginCommand(request.email(), request.password()));
    return withSession(session);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Renovar el token de acceso",
      description =
          "Usa solo la cookie HttpOnly. Rota el token de refresco; reutilizar uno ya usado revoca"
              + " toda su familia.")
  ResponseEntity<?> refresh(
      @CookieValue(name = "${trato.identity.refresh-cookie.name}", required = false)
          String refreshToken) {
    try {
      return withSession(refreshSession.execute(refreshToken));
    } catch (RefreshTokenReuseException | SessionExpiredException e) {
      // La sesión terminó: además del 401 se borra la cookie del navegador.
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
          .body(ProblemDetails.of(HttpStatus.UNAUTHORIZED, e.code(), e.getMessage()));
    }
  }

  @PostMapping("/logout")
  @Operation(summary = "Cerrar sesión revocando el token de refresco vigente")
  @SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
  ResponseEntity<Void> logout(
      @AuthenticationPrincipal Jwt jwt,
      @CookieValue(name = "${trato.identity.refresh-cookie.name}", required = false)
          String refreshToken) {
    logout.execute(UserId.of(jwt.getSubject()), refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
        .build();
  }

  @PostMapping("/password/forgot")
  @Operation(summary = "Solicitar enlace de recuperación (CU-16); la respuesta es siempre igual")
  ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
    requestPasswordReset.execute(request.email());
    return ResponseEntity.accepted().body(new MessageResponse(RESET_SENT));
  }

  @PostMapping("/password/reset")
  @Operation(summary = "Definir una nueva contraseña con el enlace; revoca todas las sesiones")
  MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    resetPassword.execute(new ResetPasswordCommand(request.token(), request.password()));
    return new MessageResponse("Tu contraseña cambió. Inicia sesión con la nueva contraseña.");
  }

  private ResponseEntity<TokenResponse> withSession(AuthSession session) {
    var now = clock.instant();
    long expiresIn = Duration.between(now, session.accessTokenExpiresAt()).toSeconds();
    return ResponseEntity.status(HttpStatus.OK)
        .header(
            HttpHeaders.SET_COOKIE,
            cookies.issue(session.refreshToken(), session.refreshTokenExpiresAt(), now).toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(
            new TokenResponse(
                session.accessToken(), "Bearer", expiresIn, session.accessTokenExpiresAt()));
  }
}
