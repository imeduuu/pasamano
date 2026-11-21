package cl.trato.shared.infrastructure.events;

import cl.trato.shared.application.DomainEventPublisher;
import cl.trato.shared.domain.event.DomainEvent;
import java.util.Collection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publica los eventos de dominio como eventos de aplicación de Spring (ADR-008). */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher publisher;

  public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(Collection<? extends DomainEvent> events) {
    events.forEach(publisher::publishEvent);
  }
}
