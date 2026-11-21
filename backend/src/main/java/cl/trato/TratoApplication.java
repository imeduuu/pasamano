package cl.trato;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Punto de entrada del monolito modular Trato. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TratoApplication {

  public static void main(String[] args) {
    // Todas las fechas del backend se manejan en UTC (RNF-I18N-01).
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(TratoApplication.class, args);
  }
}
