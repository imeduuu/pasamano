package cl.trato.catalog.domain;

/** Estados del ciclo de vida de una publicación (sección 8.6 del SRS). */
public enum ListingStatus {
  BORRADOR,
  ACTIVA,
  PAUSADA,
  AGOTADA,
  BLOQUEADA,
  ELIMINADA
}
