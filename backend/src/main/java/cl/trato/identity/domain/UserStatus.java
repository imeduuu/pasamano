package cl.trato.identity.domain;

/** Estado de la cuenta. Equivalencias con el SRS entre paréntesis. */
public enum UserStatus {
  /** Registrada, con el correo aún sin verificar (PENDIENTE_VERIFICACION). */
  PENDING_VERIFICATION,
  /** Verificada y habilitada (ACTIVA). */
  ACTIVE,
  /** Suspendida por un administrador; no puede iniciar sesión, RN-17 (SUSPENDIDA). */
  SUSPENDED
}
