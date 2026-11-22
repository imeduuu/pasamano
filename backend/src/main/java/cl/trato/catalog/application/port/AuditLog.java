package cl.trato.catalog.application.port;

import java.time.Instant;
import java.util.UUID;

/** Puerto para registrar acciones administrativas (RF-ADM-05). */
public interface AuditLog {
  void record(UUID adminId, String action, String resource, String reason, Instant occurredAt);
}
