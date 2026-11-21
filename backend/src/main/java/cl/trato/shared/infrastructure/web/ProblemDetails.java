package cl.trato.shared.infrastructure.web;

import cl.trato.shared.domain.exception.ErrorCategory;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Construye respuestas Problem Details (RFC 9457) con un {@code code} estable y el {@code
 * correlationId} de la petición. Los títulos y detalles están en español (RNF-USA-04).
 */
public final class ProblemDetails {

  public static final String CODE = "code";
  public static final String CORRELATION_ID = "correlationId";
  public static final String ERRORS = "errors";

  private static final String TYPE_PREFIX = "urn:trato:problem:";

  private ProblemDetails() {}

  public static ProblemDetail of(HttpStatus status, String code, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    decorate(problem, code);
    return problem;
  }

  /** Completa título, tipo, código y correlationId sin pisar lo que ya tenga el problema. */
  public static ProblemDetail decorate(ProblemDetail problem, String code) {
    HttpStatus status = HttpStatus.resolve(problem.getStatus());
    problem.setTitle(titleFor(status));
    problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
    problem.setProperty(CODE, code);
    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
    if (correlationId != null) {
      problem.setProperty(CORRELATION_ID, correlationId);
    }
    return problem;
  }

  /** Código genérico para errores sin código de negocio (por ejemplo, METHOD_NOT_ALLOWED). */
  public static String defaultCode(int status) {
    HttpStatus resolved = HttpStatus.resolve(status);
    return resolved == null ? "HTTP_" + status : resolved.name();
  }

  public static HttpStatus statusFor(ErrorCategory category) {
    return switch (category) {
      case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
      case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_CONTENT;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case LOCKED -> HttpStatus.LOCKED;
    };
  }

  /** Representación plana para escribir el problema fuera de Spring MVC (filtros). */
  public static Map<String, Object> toMap(ProblemDetail problem) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", problem.getType().toString());
    body.put("title", problem.getTitle());
    body.put("status", problem.getStatus());
    if (problem.getDetail() != null) {
      body.put("detail", problem.getDetail());
    }
    if (problem.getInstance() != null) {
      body.put("instance", problem.getInstance().toString());
    }
    if (problem.getProperties() != null) {
      body.putAll(problem.getProperties());
    }
    return body;
  }

  static String titleFor(HttpStatus status) {
    if (status == null) {
      return "Error";
    }
    return switch (status) {
      case BAD_REQUEST -> "Solicitud inválida";
      case UNAUTHORIZED -> "No autenticado";
      case FORBIDDEN -> "Acceso denegado";
      case NOT_FOUND -> "Recurso no encontrado";
      case METHOD_NOT_ALLOWED -> "Método no permitido";
      case CONFLICT -> "Conflicto";
      case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
      case UNPROCESSABLE_CONTENT -> "Regla de negocio infringida";
      case LOCKED -> "Recurso bloqueado";
      case TOO_MANY_REQUESTS -> "Demasiadas solicitudes";
      case INTERNAL_SERVER_ERROR -> "Error interno";
      case SERVICE_UNAVAILABLE -> "Servicio no disponible";
      default -> status.getReasonPhrase();
    };
  }
}
