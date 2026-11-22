package cl.trato.catalog.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
class AuditLogJpaEntity {
  @Id private UUID id;

  @Column(name = "admin_id", nullable = false)
  private UUID adminId;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(nullable = false, length = 120)
  private String resource;

  @Column(length = 500)
  private String reason;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AuditLogJpaEntity() {}

  AuditLogJpaEntity(
      UUID id, UUID adminId, String action, String resource, String reason, Instant occurredAt) {
    this.id = id;
    this.adminId = adminId;
    this.action = action;
    this.resource = resource;
    this.reason = reason;
    this.occurredAt = occurredAt;
  }
}
