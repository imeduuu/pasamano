package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.application.port.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaAuditLogAdapter implements AuditLog {
  private final AuditLogJpaRepository repository;

  JpaAuditLogAdapter(AuditLogJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void record(
      UUID adminId, String action, String resource, String reason, Instant occurredAt) {
    repository.save(
        new AuditLogJpaEntity(UUID.randomUUID(), adminId, action, resource, reason, occurredAt));
  }
}
