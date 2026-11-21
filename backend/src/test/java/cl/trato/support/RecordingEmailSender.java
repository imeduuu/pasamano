package cl.trato.support;

import cl.trato.notifications.application.EmailMessage;
import cl.trato.notifications.application.port.EmailSender;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reemplaza el SMTP en las IT: guarda los correos para leer los enlaces que contienen. */
public class RecordingEmailSender implements EmailSender {

  private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

  private final ConcurrentLinkedQueue<EmailMessage> sent = new ConcurrentLinkedQueue<>();

  @Override
  public void send(EmailMessage message) {
    sent.add(message);
  }

  public List<EmailMessage> to(String email) {
    return sent.stream().filter(m -> m.to().equalsIgnoreCase(email)).toList();
  }

  /** Espera (el envío es asíncrono) hasta que llegue un correo con ese asunto. */
  public EmailMessage await(String email, String subjectFragment) {
    Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
    while (Instant.now().isBefore(deadline)) {
      Optional<EmailMessage> found =
          to(email).stream().filter(m -> m.subject().contains(subjectFragment)).reduce((a, b) -> b);
      if (found.isPresent()) {
        return found.get();
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    throw new AssertionError("No llegó un correo '" + subjectFragment + "' para " + email);
  }

  public String awaitToken(String email, String subjectFragment) {
    Matcher matcher = TOKEN.matcher(await(email, subjectFragment).textBody());
    if (!matcher.find()) {
      throw new AssertionError("El correo no contiene un token");
    }
    return matcher.group(1);
  }
}
