package cl.trato.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.notifications.application.port.EmailSender;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RNF-FIA-04: los correos se reintentan con espera exponencial (3 intentos). */
class EmailDeliveryTest {

  static final EmailMessage MESSAGE = new EmailMessage("ana@ejemplo.cl", "Asunto", "Texto", null);

  final List<Duration> sleeps = new ArrayList<>();
  final RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1), 2.0);

  static final class FlakySender implements EmailSender {
    int failuresLeft;
    int calls;

    FlakySender(int failures) {
      this.failuresLeft = failures;
    }

    @Override
    public void send(EmailMessage message) {
      calls++;
      if (failuresLeft-- > 0) {
        throw new IllegalStateException("SMTP caído");
      }
    }
  }

  @Test
  void sends_on_the_first_try_without_waiting() {
    FlakySender sender = new FlakySender(0);

    boolean delivered = new EmailDelivery(sender, policy, sleeps::add).deliver(MESSAGE);

    assertThat(delivered).isTrue();
    assertThat(sender.calls).isEqualTo(1);
    assertThat(sleeps).isEmpty();
  }

  @Test
  void retries_with_exponential_backoff_until_it_succeeds() {
    FlakySender sender = new FlakySender(2);

    boolean delivered = new EmailDelivery(sender, policy, sleeps::add).deliver(MESSAGE);

    assertThat(delivered).isTrue();
    assertThat(sender.calls).isEqualTo(3);
    assertThat(sleeps).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
  }

  @Test
  void gives_up_after_three_attempts_without_throwing() {
    FlakySender sender = new FlakySender(10);

    boolean delivered = new EmailDelivery(sender, policy, sleeps::add).deliver(MESSAGE);

    assertThat(delivered).isFalse();
    assertThat(sender.calls).isEqualTo(3);
  }

  @Test
  void stops_retrying_if_the_thread_is_interrupted() {
    FlakySender sender = new FlakySender(10);
    EmailDelivery delivery =
        new EmailDelivery(
            sender,
            policy,
            d -> {
              throw new InterruptedException();
            });

    assertThat(delivery.deliver(MESSAGE)).isFalse();
    assertThat(sender.calls).isEqualTo(1);
    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void the_retry_policy_computes_the_delay_before_each_retry() {
    assertThat(policy.delayBeforeRetry(1)).isEqualTo(Duration.ofSeconds(1));
    assertThat(policy.delayBeforeRetry(2)).isEqualTo(Duration.ofSeconds(2));
    assertThat(policy.delayBeforeRetry(3)).isEqualTo(Duration.ofSeconds(4));
  }

  @Test
  void the_retry_policy_needs_at_least_one_attempt() {
    assertThatThrownBy(() -> new RetryPolicy(0, Duration.ZERO, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RetryPolicy(1, Duration.ofSeconds(-1), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RetryPolicy(1, Duration.ZERO, 0.5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void a_message_needs_recipient_subject_and_text() {
    assertThatThrownBy(() -> new EmailMessage(" ", "s", "t", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmailMessage("a@b.cl", "", "t", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmailMessage("a@b.cl", "s", null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
