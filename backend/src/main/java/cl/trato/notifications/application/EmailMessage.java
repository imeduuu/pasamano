package cl.trato.notifications.application;

/**
 * Correo transaccional listo para enviar.
 *
 * @param to destinatario
 * @param subject asunto
 * @param textBody cuerpo en texto plano
 * @param htmlBody cuerpo HTML opcional
 */
public record EmailMessage(String to, String subject, String textBody, String htmlBody) {

  public EmailMessage {
    if (to == null || to.isBlank()) {
      throw new IllegalArgumentException("El destinatario es obligatorio");
    }
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("El asunto es obligatorio");
    }
    if (textBody == null) {
      throw new IllegalArgumentException("El cuerpo es obligatorio");
    }
  }

  /** El cuerpo incluye enlaces con tokens: nunca se imprime. */
  @Override
  public String toString() {
    return "EmailMessage[subject=" + subject + "]";
  }
}
