package cl.trato.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generates_an_id_when_the_request_has_none_and_exposes_it_in_mdc_and_response()
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInMdc = new AtomicReference<>();
    FilterChain chain = (req, res) -> seenInMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

    filter.doFilter(request, response, chain);

    String header = response.getHeader(CorrelationIdFilter.HEADER);
    assertThat(header).isNotBlank().hasSize(36);
    assertThat(seenInMdc.get()).isEqualTo(header);
  }

  @Test
  void reuses_a_valid_incoming_id() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "abc-12345-xyz");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-12345-xyz");
  }

  @Test
  void ignores_incoming_ids_that_could_inject_content_into_logs() {
    assertThat(CorrelationIdFilter.resolve("bad\nvalue-1234")).isNotEqualTo("bad\nvalue-1234");
    assertThat(CorrelationIdFilter.resolve("short")).hasSize(36);
    assertThat(CorrelationIdFilter.resolve("x".repeat(65))).hasSize(36);
  }

  @Test
  void cleans_the_mdc_after_the_request() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {});

    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }
}
