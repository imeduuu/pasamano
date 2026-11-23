package cl.trato.geo.infrastructure.in.web;

import cl.trato.geo.application.GetRegions;
import cl.trato.geo.application.RegionView;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública de regiones de Chile con sus comunas (RF-PUB-03). */
@RestController
@RequestMapping("/api/v1/regions")
@Tag(name = "Regiones y comunas")
class RegionsController {
  private final GetRegions getRegions;

  RegionsController(GetRegions getRegions) {
    this.getRegions = getRegions;
  }

  @GetMapping
  List<RegionView> get() {
    return getRegions.execute();
  }
}
