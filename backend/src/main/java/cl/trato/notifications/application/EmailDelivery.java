package cl.trato.notifications.application;

import cl.trato.notifications.application.port.EmailSender;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrega un correo con reintentos y espera exponencial (RNF-FIA-04). Nunca propaga la falla: la
 * operación que originó el correo ya se confirmó (RF-NOT-02).
 */
public class EmailDelivery {

  private static final Logger LOG = LoggerFactory.getLogger(EmailDelivery.class);

  private final EmailSender sender;
  private final RetryPolicy policy;
  private final Sleeper sleeper;

  public EmailDelivery(EmailSender sender, RetryPolicy policy, Sleeper sleeper) {
    this.sender = sender;
    this.policy = policy;
    this.sleeper = sleeper;
  }

  /** Devuelve {@code true} si el correo se envió en alguno de los intentos. */
  public boolean deliver(EmailMessage message) {
    for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
      try {
        sender.send(message);
        return true;
      } catch (RuntimeException e) {
        LOG.warn(
            "Falló el envío del correo '{}' (intento {} de {}): {}",
            message.subject(),
            attempt,
            policy.maxAttempts(),
            e.getMessage());
      }
      if (attempt < policy.maxAttempts() && !pause(policy.delayBeforeRetry(attempt))) {
        return false;
      }
    }
    LOG.error(
        "No se pudo enviar el correo '{}' tras {} intentos",
        message.subject(),
        policy.maxAttempts());
    return false;
  }

  private boolean pause(Duration delay) {
    try {
      sleeper.sleep(delay);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
