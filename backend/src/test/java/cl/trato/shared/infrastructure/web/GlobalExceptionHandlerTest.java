package cl.trato.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.ConflictException;
import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.DomainValidationException;
import cl.trato.shared.domain.exception.ErrorCategory;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  static Stream<Arguments> domainExceptions() {
    return Stream.of(
        Arguments.of(new DomainValidationException("BAD_VALUE", "x"), HttpStatus.BAD_REQUEST),
        Arguments.of(
            new BusinessRuleViolationException("RULE_BROKEN", "x"),
            HttpStatus.UNPROCESSABLE_CONTENT),
        Arguments.of(new ResourceNotFoundException("NOT_THERE", "x"), HttpStatus.NOT_FOUND),
        Arguments.of(new ConflictException("ALREADY_THERE", "x"), HttpStatus.CONFLICT),
        Arguments.of(custom(ErrorCategory.AUTHENTICATION), HttpStatus.UNAUTHORIZED),
        Arguments.of(custom(ErrorCategory.FORBIDDEN), HttpStatus.FORBIDDEN),
        Arguments.of(custom(ErrorCategory.LOCKED), HttpStatus.LOCKED));
  }

  private static DomainException custom(ErrorCategory category) {
    return new DomainException("CUSTOM_CODE", category, "x") {};
  }

  @ParameterizedTest
  @MethodSource("domainExceptions")
  void maps_each_error_category_to_its_http_status(DomainException ex, HttpStatus expected) {
    ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

    assertThat(response.getStatusCode()).isEqualTo(expected);
    assertThat(response.getBody().getStatus()).isEqualTo(expected.value());
  }

  @Test
  void the_problem_carries_stable_code_spanish_detail_type_and_correlation_id() {
    MDC.put(CorrelationIdFilter.MDC_KEY, "corr-1234-5678");

    ProblemDetail problem =
        handler
            .handleDomainException(
                new BusinessRuleViolationException("OWN_LISTING", "No puedes comprar lo tuyo."))
            .getBody();

    assertThat(problem.getDetail()).isEqualTo("No puedes comprar lo tuyo.");
    assertThat(problem.getTitle()).isEqualTo("Regla de negocio infringida");
    assertThat(problem.getType()).hasToString("urn:trato:problem:own-listing");
    assertThat(problem.getProperties())
        .containsEntry("code", "OWN_LISTING")
        .containsEntry("correlationId", "corr-1234-5678");
  }

  @Test
  void unexpected_errors_never_leak_technical_details() {
    ProblemDetail problem =
        handler.handleUnexpected(new IllegalStateException("NullPointer en la línea 42")).getBody();

    assertThat(problem.getStatus()).isEqualTo(500);
    assertThat(problem.getDetail()).doesNotContain("NullPointer");
    assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
  }

  @Test
  void security_exceptions_raised_in_controllers_keep_their_status() {
    assertThat(handler.handleAuthentication(new BadCredentialsException("x")).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(handler.handleAccessDenied(new AccessDeniedException("x")).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void problem_can_be_flattened_for_filters() {
    ProblemDetail problem =
        ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Espera");

    Map<String, Object> body = ProblemDetails.toMap(problem);

    assertThat(body)
        .containsEntry("status", 429)
        .containsEntry("title", "Demasiadas solicitudes")
        .containsEntry("code", "RATE_LIMITED")
        .containsEntry("detail", "Espera");
  }

  @Test
  void default_codes_come_from_the_http_status() {
    assertThat(ProblemDetails.defaultCode(405)).isEqualTo("METHOD_NOT_ALLOWED");
    assertThat(ProblemDetails.defaultCode(599)).isEqualTo("HTTP_599");
    assertThat(ProblemDetails.titleFor(null)).isEqualTo("Error");
    assertThat(ProblemDetails.titleFor(HttpStatus.I_AM_A_TEAPOT)).isEqualTo("I'm a teapot");
  }
}
