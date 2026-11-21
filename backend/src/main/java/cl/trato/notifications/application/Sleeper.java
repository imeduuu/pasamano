package cl.trato.notifications.application;

import java.time.Duration;

/** Espera entre reintentos; se reemplaza en pruebas para no dormir. */
@FunctionalInterface
public interface Sleeper {

  void sleep(Duration duration) throws InterruptedException;
}
