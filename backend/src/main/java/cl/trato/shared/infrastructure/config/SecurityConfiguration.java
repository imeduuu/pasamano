package cl.trato.shared.infrastructure.config;

import cl.trato.shared.infrastructure.ratelimit.RateLimiter;
import cl.trato.shared.infrastructure.security.ApiRateLimitFilter;
import cl.trato.shared.infrastructure.security.ProblemDetailsSecurityHandler;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad HTTP: API sin estado con JWT de acceso (ADR-002, RNF-ESC-01), autorización por rol
 * (RF-AUT-09), CORS restringido al frontend, cabeceras de seguridad (RNF-SEG-04) y límite general
 * de peticiones (RNF-SEG-05). Los errores 401 y 403 se responden en Problem Details.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  static final String[] PUBLIC_DOCS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

  /** Endpoints de autenticación que no requieren token de acceso. */
  static final String[] PUBLIC_AUTH = {
    "/api/v1/auth/register",
    "/api/v1/auth/verify",
    "/api/v1/auth/verify/resend",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/password/forgot",
    "/api/v1/auth/password/reset"
  };

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      ProblemDetailsSecurityHandler problemHandler,
      ObjectProvider<JwtAuthenticationConverter> jwtAuthenticationConverter,
      JsonMapper jsonMapper,
      @Value("${trato.rate-limit.api.capacity:100}") int apiCapacity,
      @Value("${trato.rate-limit.api.period:1m}") Duration apiPeriod)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            e -> e.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler))
        .oauth2ResourceServer(oauth ->
            oauth
                .authenticationEntryPoint(problemHandler)
                .accessDeniedHandler(problemHandler)
                .jwt(
                    jwt -> jwtAuthenticationConverter.ifAvailable(jwt::jwtAuthenticationConverter)))
        .headers(h ->
            h.contentSecurityPolicy(csp ->
                    csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; "
                            + "style-src 'self' 'unsafe-inline'; object-src 'none'; "
                            + "frame-ancestors 'none'"))
                .referrerPolicy(
                    r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .addFilterAfter(
            new ApiRateLimitFilter(new RateLimiter(apiCapacity, apiPeriod), jsonMapper),
            BearerTokenAuthenticationFilter.class)
        .authorizeHttpRequests(auth ->
            auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                .permitAll()
                .requestMatchers(PUBLIC_DOCS)
                .permitAll()
                .requestMatchers("/error")
                .permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_AUTH)
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated());
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${trato.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-Correlation-Id", "Idempotency-Key"));
    config.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
