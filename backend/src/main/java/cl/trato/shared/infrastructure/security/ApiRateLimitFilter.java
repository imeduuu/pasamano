package cl.trato.shared.infrastructure.security;

import cl.trato.shared.infrastructure.ratelimit.RateLimiter;
import cl.trato.shared.infrastructure.web.ProblemDetails;
import cl.trato.shared.infrastructure.web.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Límite general de la API (RNF-SEG-05: 100 peticiones por minuto por usuario). Las peticiones
 * anónimas se cuentan por IP. Se ejecuta después de autenticar el token de acceso.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

  private final RateLimiter limiter;
  private final JsonMapper jsonMapper;

  public ApiRateLimitFilter(RateLimiter limiter, JsonMapper jsonMapper) {
    this.limiter = limiter;
    this.jsonMapper = jsonMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    RateLimiter.Decision decision = limiter.tryConsume(keyFor(request));
    if (decision.allowed()) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(
        response.getOutputStream(),
        ProblemDetails.toMap(
            ProblemDetails.of(
                HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", RateLimitExceededException.MESSAGE)));
  }

  static String keyFor(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      return "user:" + auth.getName();
    }
    return "ip:" + request.getRemoteAddr();
  }
}
