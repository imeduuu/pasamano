package cl.trato.shared.domain.event;

import java.time.Instant;

/**
 * Hecho relevante ocurrido en el dominio. Los módulos se comunican mediante eventos en proceso que
 * se publican después del commit (ADR-008).
 */
public interface DomainEvent {

  /** Momento en que ocurrió el hecho, siempre en UTC. */
  Instant occurredAt();

  /** Nombre del tipo de evento, útil para logs y auditoría. */
  default String eventType() {
    return getClass().getSimpleName();
  }
}
