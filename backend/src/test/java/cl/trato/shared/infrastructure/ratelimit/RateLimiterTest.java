package cl.trato.shared.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.infrastructure.security.ApiRateLimitFilter;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/** RNF-SEG-05: 5/min en el inicio de sesión y 100/min por usuario en la API general. */
class RateLimiterTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void allows_up_to_the_capacity_and_then_asks_to_wait() {
    RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1));

    for (int i = 0; i < 5; i++) {
      assertThat(limiter.tryConsume("ip|ana@ejemplo.cl").allowed()).isTrue();
    }
    RateLimiter.Decision sixth = limiter.tryConsume("ip|ana@ejemplo.cl");

    assertThat(sixth.allowed()).isFalse();
    assertThat(sixth.retryAfterSeconds()).isBetween(1L, 61L);
  }

  @Test
  void each_key_has_its_own_bucket() {
    RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1));

    assertThat(limiter.tryConsume("a").allowed()).isTrue();
    assertThat(limiter.tryConsume("a").allowed()).isFalse();
    assertThat(limiter.tryConsume("b").allowed()).isTrue();
  }

  @Test
  void old_keys_are_evicted_to_bound_memory() {
    RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), 2);
    limiter.tryConsume("a");
    limiter.tryConsume("b");
    limiter.tryConsume("c");

    assertThat(limiter.tryConsume("a").allowed()).isTrue();
  }

  @Test
  void rejects_invalid_configuration() {
    assertThatThrownBy(() -> new RateLimiter(0, Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RateLimiter(1, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void the_api_filter_limits_each_authenticated_user_and_answers_429_problem_details()
      throws Exception {
    ApiRateLimitFilter filter =
        new ApiRateLimitFilter(
            new RateLimiter(2, Duration.ofMinutes(1)), JsonMapper.builder().build());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    AtomicInteger passed = new AtomicInteger();
    FilterChain chain = (req, res) -> passed.incrementAndGet();

    MockHttpServletResponse last = null;
    for (int i = 0; i < 3; i++) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
      last = new MockHttpServletResponse();
      filter.doFilter(request, last, chain);
    }

    assertThat(passed).hasValue(2);
    assertThat(last.getStatus()).isEqualTo(429);
    assertThat(last.getHeader("Retry-After")).isNotBlank();
    assertThat(last.getContentType()).isEqualTo("application/problem+json");
    assertThat(last.getContentAsString()).contains("\"code\":\"RATE_LIMITED\"");
  }

  @Test
  void the_api_filter_counts_anonymous_requests_by_ip_and_ignores_non_api_paths() throws Exception {
    ApiRateLimitFilter filter =
        new ApiRateLimitFilter(
            new RateLimiter(1, Duration.ofMinutes(1)), JsonMapper.builder().build());
    AtomicInteger passed = new AtomicInteger();
    FilterChain chain = (req, res) -> passed.incrementAndGet();

    for (int i = 0; i < 3; i++) {
      filter.doFilter(
          new MockHttpServletRequest("GET", "/actuator/health"),
          new MockHttpServletResponse(),
          chain);
    }
    MockHttpServletRequest fromA = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    fromA.setRemoteAddr("10.0.0.1");
    MockHttpServletRequest fromB = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    fromB.setRemoteAddr("10.0.0.2");
    filter.doFilter(fromA, new MockHttpServletResponse(), chain);
    filter.doFilter(fromB, new MockHttpServletResponse(), chain);

    assertThat(passed).hasValue(5);
  }
}
