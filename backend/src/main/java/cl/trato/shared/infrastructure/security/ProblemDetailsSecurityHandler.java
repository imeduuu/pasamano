package cl.trato.shared.infrastructure.security;

import cl.trato.shared.infrastructure.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Responde 401 y 403 de la cadena de seguridad en formato Problem Details. */
@Component
public class ProblemDetailsSecurityHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final JsonMapper jsonMapper;

  public ProblemDetailsSecurityHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    write(
        response,
        ProblemDetails.of(
            HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Debes iniciar sesión para continuar."));
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(
        response,
        ProblemDetails.of(
            HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tienes permiso para esta operación."));
  }

  private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
    response.setStatus(problem.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getOutputStream(), ProblemDetails.toMap(problem));
  }
}
