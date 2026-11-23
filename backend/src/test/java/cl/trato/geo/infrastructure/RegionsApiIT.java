package cl.trato.geo.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.trato.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** API pública {@code GET /api/v1/regions} con regiones y comunas (RF-PUB-03). */
@IntegrationTest
class RegionsApiIT {
  @Autowired MockMvc mvc;

  @Test
  void returns_regions_from_north_to_south_with_their_communes_without_authentication()
      throws Exception {
    mvc.perform(get("/api/v1/regions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(16))
        .andExpect(jsonPath("$[0].code").value("15"))
        .andExpect(jsonPath("$[0].name").value("Arica y Parinacota"))
        .andExpect(jsonPath("$[0].communes.length()").value(4))
        .andExpect(jsonPath("$[0].communes[0].code").value("15101"))
        .andExpect(jsonPath("$[0].communes[0].name").value("Arica"))
        .andExpect(jsonPath("$[6].code").value("13"))
        .andExpect(jsonPath("$[6].communes.length()").value(52))
        .andExpect(jsonPath("$[6].communes[?(@.code == '13101')].name").value("Santiago"))
        .andExpect(jsonPath("$[15].code").value("12"));
  }
}
