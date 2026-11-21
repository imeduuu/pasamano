package cl.trato.shared.application;

import cl.trato.shared.domain.event.DomainEvent;
import java.util.Collection;

/**
 * Puerto de salida para publicar eventos de dominio. Los suscriptores reaccionan después del commit
 * de la transacción que los originó (ADR-008).
 */
public interface DomainEventPublisher {

  void publish(Collection<? extends DomainEvent> events);
}
