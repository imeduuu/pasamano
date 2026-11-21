package cl.trato.shared.infrastructure.web;

import cl.trato.shared.domain.exception.DomainException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce toda excepción de la API a Problem Details (RFC 9457) con un {@code code} estable. Nunca
 * expone trazas técnicas (RNF-USA-04).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
    HttpStatus status = ProblemDetails.statusFor(ex.category());
    LOG.debug("Error de dominio {}: {}", ex.code(), ex.getMessage());
    return ResponseEntity.status(status)
        .body(ProblemDetails.of(status, ex.code(), ex.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ProblemDetails.of(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Debes iniciar sesión para continuar."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ProblemDetails.of(
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tienes permiso para esta operación."));
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
        .body(ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage()));
  }

  /** Dos operaciones modificaron el mismo recurso a la vez (control optimista por versión). */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ProblemDetails.of(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                "El recurso cambió mientras lo editabas. Vuelve a intentarlo."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
    LOG.error("Error no controlado", ex);
    return ResponseEntity.internalServerError()
        .body(
            ProblemDetails.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Intenta nuevamente más tarde."));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error ->
                Map.of(
                    "field", error.getField(),
                    "message", String.valueOf(error.getDefaultMessage())))
            .toList();
    ProblemDetail problem =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Algunos datos no son válidos. Revisa los campos indicados.");
    problem.setProperty(ProblemDetails.ERRORS, errors);
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "El cuerpo de la solicitud no es válido o está incompleto.");
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  /** Agrega código y correlationId a los errores estándar de Spring MVC (404, 405, 415...). */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (response != null && response.getBody() instanceof ProblemDetail problem) {
      Object code =
          problem.getProperties() == null ? null : problem.getProperties().get(ProblemDetails.CODE);
      if (code == null) {
        ProblemDetails.decorate(problem, ProblemDetails.defaultCode(problem.getStatus()));
      }
    }
    return response;
  }
}
