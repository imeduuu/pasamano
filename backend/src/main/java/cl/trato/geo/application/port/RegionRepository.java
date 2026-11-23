package cl.trato.geo.application.port;

import cl.trato.geo.domain.Region;
import java.util.List;

/** Puerto de lectura de los datos de referencia de regiones y comunas. */
public interface RegionRepository {
  /** Todas las regiones con sus comunas, de norte a sur. */
  List<Region> findAll();
}
