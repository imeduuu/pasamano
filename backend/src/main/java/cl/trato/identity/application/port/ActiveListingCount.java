package cl.trato.identity.application.port;

import cl.trato.identity.domain.UserId;

/** Puerto para consultar publicaciones activas del catálogo (RF-USR-03). */
public interface ActiveListingCount {

  long countForSeller(UserId sellerId);
}
