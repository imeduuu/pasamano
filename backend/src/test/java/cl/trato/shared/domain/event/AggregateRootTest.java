package cl.trato.shared.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

  record SomethingHappened(String what, Instant occurredAt) implements DomainEvent {}

  static final class SampleAggregate extends AggregateRoot {
    void doSomething(String what) {
      registerEvent(new SomethingHappened(what, Instant.parse("2026-11-21T10:00:00Z")));
    }

    void record(DomainEvent event) {
      registerEvent(event);
    }
  }

  @Test
  void collects_the_events_registered_by_the_aggregate_in_order() {
    SampleAggregate aggregate = new SampleAggregate();

    aggregate.doSomething("primero");
    aggregate.doSomething("segundo");

    assertThat(aggregate.pullDomainEvents())
        .extracting(event -> ((SomethingHappened) event).what())
        .containsExactly("primero", "segundo");
  }

  @Test
  void pulling_events_clears_them() {
    SampleAggregate aggregate = new SampleAggregate();
    aggregate.doSomething("una vez");

    aggregate.pullDomainEvents();

    assertThat(aggregate.pullDomainEvents()).isEmpty();
  }

  @Test
  void pulled_events_cannot_be_modified() {
    SampleAggregate aggregate = new SampleAggregate();
    aggregate.doSomething("x");

    var events = aggregate.pullDomainEvents();

    assertThatThrownBy(events::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejects_null_events() {
    SampleAggregate aggregate = new SampleAggregate();

    assertThatThrownBy(() -> aggregate.record(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void the_event_type_defaults_to_the_simple_class_name() {
    DomainEvent event = new SomethingHappened("x", Instant.EPOCH);

    assertThat(event.eventType()).isEqualTo("SomethingHappened");
  }
}
