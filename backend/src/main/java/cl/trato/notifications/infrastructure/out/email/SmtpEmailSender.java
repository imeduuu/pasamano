package cl.trato.notifications.infrastructure.out.email;

import cl.trato.notifications.application.EmailMessage;
import cl.trato.notifications.application.port.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Adaptador SMTP del puerto {@link EmailSender} (Mailpit en local, proveedor SMTP en demo). */
public class SmtpEmailSender implements EmailSender {

  private final JavaMailSender mailSender;
  private final String from;

  public SmtpEmailSender(JavaMailSender mailSender, String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  @Override
  public void send(EmailMessage message) {
    MimeMessage mime = mailSender.createMimeMessage();
    try {
      boolean multipart = message.htmlBody() != null;
      MimeMessageHelper helper =
          new MimeMessageHelper(mime, multipart, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(message.to());
      helper.setSubject(message.subject());
      if (multipart) {
        helper.setText(message.textBody(), message.htmlBody());
      } else {
        helper.setText(message.textBody(), false);
      }
    } catch (MessagingException e) {
      throw new MailPreparationException("No se pudo preparar el correo", e);
    }
    mailSender.send(mime);
  }
}
