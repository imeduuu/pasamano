package cl.trato.shared.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Limitador de peticiones por clave con Bucket4j (RNF-SEG-05). Los baldes viven en memoria con un
 * máximo de claves (LRU) para no crecer sin límite; con varias réplicas el límite es por instancia.
 */
public class RateLimiter {

  private static final int DEFAULT_MAX_KEYS = 10_000;

  private final int capacity;
  private final Duration period;
  private final Map<String, Bucket> buckets;

  public RateLimiter(int capacity, Duration period) {
    this(capacity, period, DEFAULT_MAX_KEYS);
  }

  public RateLimiter(int capacity, Duration period, int maxKeys) {
    if (capacity < 1 || period == null || period.isZero() || period.isNegative()) {
      throw new IllegalArgumentException("Capacidad y periodo deben ser positivos");
    }
    this.capacity = capacity;
    this.period = period;
    this.buckets =
        Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > maxKeys;
              }
            });
  }

  /** Consume un permiso para la clave; si no quedan, indica en cuántos segundos reintentar. */
  public Decision tryConsume(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return new Decision(true, 0);
    }
    long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
    return new Decision(false, Math.max(1, seconds + 1));
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, period))
        .build();
  }

  /**
   * Resultado de consumir un permiso.
   *
   * @param allowed si la petición puede continuar
   * @param retryAfterSeconds segundos a esperar si no se permitió
   */
  public record Decision(boolean allowed, long retryAfterSeconds) {}
}
