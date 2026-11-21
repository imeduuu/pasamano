package cl.trato.shared.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj del sistema en UTC; los casos de uso lo reciben inyectado para ser deterministas. */
@Configuration
public class ClockConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
