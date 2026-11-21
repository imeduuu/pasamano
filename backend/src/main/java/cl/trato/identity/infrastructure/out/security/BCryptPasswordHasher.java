package cl.trato.identity.infrastructure.out.security;

import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.domain.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hash de contraseñas con BCrypt, factor de costo 12 (RNF-SEG-01). */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

  public static final int STRENGTH = 12;

  private final BCryptPasswordEncoder encoder;
  private final String dummyHash;

  public BCryptPasswordHasher() {
    this(STRENGTH);
  }

  BCryptPasswordHasher(int strength) {
    this.encoder = new BCryptPasswordEncoder(strength);
    this.dummyHash = encoder.encode("hash-ficticio-para-igualar-tiempos");
  }

  @Override
  public PasswordHash hash(String rawPassword) {
    return new PasswordHash(encoder.encode(rawPassword));
  }

  @Override
  public boolean matches(String rawPassword, PasswordHash hash) {
    if (rawPassword == null) {
      return false;
    }
    try {
      return encoder.matches(rawPassword, hash.value());
    } catch (IllegalArgumentException e) {
      // Contraseñas de más de 72 bytes: BCrypt no las admite, por lo que nunca coinciden.
      return false;
    }
  }

  @Override
  public void matchesDummy(String rawPassword) {
    matches(rawPassword == null ? "" : rawPassword, new PasswordHash(dummyHash));
  }
}
