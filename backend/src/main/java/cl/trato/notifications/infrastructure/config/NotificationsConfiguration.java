package cl.trato.notifications.infrastructure.config;

import cl.trato.notifications.application.EmailDelivery;
import cl.trato.notifications.application.IdentityEmailTemplates;
import cl.trato.notifications.application.IdentityNotifications;
import cl.trato.notifications.application.RetryPolicy;
import cl.trato.notifications.application.port.EmailSender;
import cl.trato.notifications.infrastructure.out.email.SmtpEmailSender;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Envío asíncrono de correos con reintentos (RF-NOT-02, RNF-FIA-04). */
@Configuration
@EnableAsync
public class NotificationsConfiguration {

  public static final String EMAIL_EXECUTOR = "emailExecutor";

  @Bean(name = EMAIL_EXECUTOR)
  Executor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("email-");
    executor.setTaskDecorator(new MdcPropagatingDecorator());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(20);
    executor.initialize();
    return executor;
  }

  @Bean
  EmailSender emailSender(
      JavaMailSender mailSender,
      @Value("${trato.mail.from:Trato <no-responder@trato.localhost>}") String from) {
    return new SmtpEmailSender(mailSender, from);
  }

  @Bean
  EmailDelivery emailDelivery(
      EmailSender sender,
      @Value("${trato.mail.retry.max-attempts:3}") int maxAttempts,
      @Value("${trato.mail.retry.initial-delay:2s}") Duration initialDelay,
      @Value("${trato.mail.retry.multiplier:2.0}") double multiplier) {
    return new EmailDelivery(
        sender,
        new RetryPolicy(maxAttempts, initialDelay, multiplier),
        duration -> Thread.sleep(duration));
  }

  @Bean
  IdentityNotifications identityNotifications(
      EmailDelivery delivery, @Value("${trato.frontend.base-url}") String frontendBaseUrl) {
    return new IdentityNotifications(delivery, new IdentityEmailTemplates(frontendBaseUrl));
  }

  /** Copia el MDC (correlationId) al hilo que envía el correo, para trazar los logs. */
  static final class MdcPropagatingDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
      Map<String, String> context = MDC.getCopyOfContextMap();
      return () -> {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context != null) {
          MDC.setContextMap(context);
        }
        try {
          runnable.run();
        } finally {
          if (previous == null) {
            MDC.clear();
          } else {
            MDC.setContextMap(previous);
          }
        }
      };
    }
  }
}
