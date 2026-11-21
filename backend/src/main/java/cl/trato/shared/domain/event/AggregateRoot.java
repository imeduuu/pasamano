package cl.trato.shared.domain.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz de agregado que acumula los eventos de dominio producidos por sus operaciones. La capa de
 * aplicación los retira con {@link #pullDomainEvents()} y los publica.
 */
public abstract class AggregateRoot {

  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected void registerEvent(DomainEvent event) {
    domainEvents.add(Objects.requireNonNull(event, "event"));
  }

  /** Devuelve los eventos pendientes en el orden en que ocurrieron y los descarta. */
  public List<DomainEvent> pullDomainEvents() {
    List<DomainEvent> events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }
}
