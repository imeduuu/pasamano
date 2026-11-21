package cl.trato.identity.domain;

import java.time.Duration;

/** Propósito de un token de un solo uso y su vigencia. */
public enum TokenPurpose {
  /** Verificación de correo: 24 horas (RF-AUT-02). */
  EMAIL_VERIFICATION(Duration.ofHours(24)),
  /** Recuperación de contraseña: 1 hora (RF-AUT-06). */
  PASSWORD_RESET(Duration.ofHours(1));

  private final Duration validity;

  TokenPurpose(Duration validity) {
    this.validity = validity;
  }

  public Duration validity() {
    return validity;
  }
}
