package cl.trato.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.shared.infrastructure.web.CorrelationIdFilter;
import cl.trato.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Criterios de la plataforma del Sprint 0: salud, documentación, errores y migraciones. */
@IntegrationTest
class PlatformIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void health_is_up_and_public() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void readiness_and_liveness_probes_are_exposed() throws Exception {
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
  }

  @Test
  void openapi_contract_and_swagger_ui_are_public() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("Trato API"));
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void protected_endpoints_answer_401_as_problem_details_with_code_and_correlation_id()
      throws Exception {
    mvc.perform(get("/api/v1/anything").header(CorrelationIdFilter.HEADER, "test-corr-0001"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Content-Type", containsString("application/problem+json")))
        .andExpect(header().string(CorrelationIdFilter.HEADER, "test-corr-0001"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.correlationId").value("test-corr-0001"))
        .andExpect(jsonPath("$.title").value("No autenticado"));
  }

  @Test
  void every_response_carries_a_correlation_id() throws Exception {
    mvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(CorrelationIdFilter.HEADER, notNullValue()));
  }

  @Test
  void security_headers_are_present() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Content-Security-Policy", containsString("default-src")));
  }

  @Test
  void flyway_applied_the_baseline_migration() {
    Integer applied =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where version = '1' and success",
            Integer.class);

    assertThat(applied).isEqualTo(1);
  }
}
