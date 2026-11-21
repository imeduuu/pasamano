package cl.trato.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL real en un contenedor para las pruebas de integración. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  public static final DockerImageName POSTGRES = DockerImageName.parse("postgres:16-alpine");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(POSTGRES);
  }
}
