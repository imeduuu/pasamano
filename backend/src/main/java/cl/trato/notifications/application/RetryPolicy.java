package cl.trato.notifications.application;

import java.time.Duration;

/**
 * Política de reintentos con espera exponencial (RNF-FIA-04).
 *
 * @param maxAttempts intentos totales, incluido el primero
 * @param initialDelay espera antes del primer reintento
 * @param multiplier factor de crecimiento de la espera
 */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier) {

  public RetryPolicy {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("Debe haber al menos un intento");
    }
    if (initialDelay == null || initialDelay.isNegative()) {
      throw new IllegalArgumentException("La espera inicial no puede ser negativa");
    }
    if (multiplier < 1.0) {
      throw new IllegalArgumentException("El multiplicador debe ser al menos 1");
    }
  }

  /** Espera antes del reintento número {@code retry} (1 = primer reintento). */
  public Duration delayBeforeRetry(int retry) {
    double factor = Math.pow(multiplier, retry - 1.0);
    return Duration.ofMillis(Math.round(initialDelay.toMillis() * factor));
  }
}
