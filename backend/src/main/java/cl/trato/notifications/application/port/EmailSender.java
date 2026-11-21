package cl.trato.notifications.application.port;

import cl.trato.notifications.application.EmailMessage;

/** Puerto de salida: envío de correos (SMTP; Mailpit en local). */
@FunctionalInterface
public interface EmailSender {

  /** Envía el mensaje o lanza una excepción si el servidor de correo falla. */
  void send(EmailMessage message);
}
