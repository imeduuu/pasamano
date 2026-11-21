package cl.trato.shared.infrastructure.web;

/** Se superó el límite de peticiones; la API responde 429 con {@code Retry-After}. */
public class RateLimitExceededException extends RuntimeException {

  public static final String MESSAGE =
      "Hiciste demasiados intentos. Espera un momento antes de volver a intentarlo.";

  private final long retryAfterSeconds;

  public RateLimitExceededException(long retryAfterSeconds) {
    super(MESSAGE);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
