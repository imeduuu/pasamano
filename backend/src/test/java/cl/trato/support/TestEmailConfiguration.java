package cl.trato.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** En las IT de la API los correos se registran en memoria en vez de ir al SMTP. */
@TestConfiguration(proxyBeanMethods = false)
public class TestEmailConfiguration {

  @Bean
  @Primary
  RecordingEmailSender recordingEmailSender() {
    return new RecordingEmailSender();
  }
}
