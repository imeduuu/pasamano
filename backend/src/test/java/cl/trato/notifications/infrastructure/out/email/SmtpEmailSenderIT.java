package cl.trato.notifications.infrastructure.out.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.notifications.application.EmailDelivery;
import cl.trato.notifications.application.EmailMessage;
import cl.trato.notifications.application.RetryPolicy;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Adaptador SMTP contra Mailpit real, y reintentos con el servidor de correo caído. */
@Testcontainers
class SmtpEmailSenderIT {

  @Container
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.34")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/readyz").forPort(8025));

  static JavaMailSenderImpl mailSender(String host, int port) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "2000");
    sender.getJavaMailProperties().put("mail.smtp.timeout", "2000");
    return sender;
  }

  @Test
  void delivers_a_multipart_utf8_message_to_the_smtp_server() throws Exception {
    SmtpEmailSender sender =
        new SmtpEmailSender(
            mailSender(MAILPIT.getHost(), MAILPIT.getMappedPort(1025)),
            "Trato <no-responder@trato.localhost>");

    sender.send(
        new EmailMessage(
            "ana@ejemplo.cl",
            "Verifica tu correo en Trato",
            "Hola, Ana. Ñandú y acentos: áéíóú.",
            "<p>Hola, Ana.</p>"));

    String search =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://%s:%d/api/v1/search?query=to:ana@ejemplo.cl"
                                .formatted(MAILPIT.getHost(), MAILPIT.getMappedPort(8025))))
                    .build(),
                HttpResponse.BodyHandlers.ofString())
            .body();
    List<String> subjects = JsonPath.read(search, "$.messages[*].Subject");
    assertThat(subjects).contains("Verifica tu correo en Trato");
  }

  @Test
  void a_down_smtp_server_fails_and_delivery_retries_three_times() {
    SmtpEmailSender down = new SmtpEmailSender(mailSender("localhost", 1), "a@b.cl");
    EmailMessage message = new EmailMessage("ana@ejemplo.cl", "Asunto", "Texto", null);

    assertThatThrownBy(() -> down.send(message)).isInstanceOf(MailException.class);

    int[] attempts = {0};
    EmailDelivery delivery =
        new EmailDelivery(
            m -> {
              attempts[0]++;
              down.send(m);
            },
            new RetryPolicy(3, Duration.ofMillis(10), 2.0),
            Thread::sleep);
    assertThat(delivery.deliver(message)).isFalse();
    assertThat(attempts[0]).isEqualTo(3);
  }
}
