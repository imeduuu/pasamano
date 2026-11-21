package cl.trato.identity.infrastructure.in.web;

import cl.trato.identity.infrastructure.config.IdentityProperties;
import cl.trato.shared.infrastructure.ratelimit.RateLimiter;
import cl.trato.shared.infrastructure.web.RateLimitExceededException;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** RNF-SEG-05: como máximo 5 intentos de inicio de sesión por minuto por IP y correo. */
@Component
class LoginRateLimiter {

  private final RateLimiter limiter;

  LoginRateLimiter(IdentityProperties properties) {
    this.limiter =
        new RateLimiter(
            properties.loginRateLimit().capacity(), properties.loginRateLimit().period());
  }

  void check(String clientIp, String email) {
    String normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    RateLimiter.Decision decision = limiter.tryConsume(clientIp + "|" + normalizedEmail);
    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfterSeconds());
    }
  }
}
