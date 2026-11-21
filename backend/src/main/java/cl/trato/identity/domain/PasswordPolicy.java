package cl.trato.identity.domain;

import cl.trato.shared.domain.exception.DomainValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Política de contraseñas (CU-01 paso 3): al menos 10 caracteres y no estar en la lista de
 * contraseñas comunes. Además se limita a 72 bytes porque BCrypt ignora lo que viene después.
 */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 10;
  public static final int MAX_BYTES = 72;

  private final Set<String> commonPasswords;

  public PasswordPolicy(Collection<String> commonPasswords) {
    this.commonPasswords =
        commonPasswords.stream()
            .map(PasswordPolicy::normalize)
            .filter(p -> !p.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
  }

  public void validate(String rawPassword) {
    String password = rawPassword == null ? "" : rawPassword;
    if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
      throw new DomainValidationException(
          "PASSWORD_TOO_SHORT", "La contraseña debe tener al menos 10 caracteres.");
    }
    if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
      throw new DomainValidationException(
          "PASSWORD_TOO_LONG", "La contraseña es demasiado larga (máximo 72 caracteres simples).");
    }
    if (commonPasswords.contains(normalize(password))) {
      throw new DomainValidationException(
          "PASSWORD_TOO_COMMON",
          "Esa contraseña es demasiado común. Elige una más difícil de adivinar.");
    }
  }

  private static String normalize(String password) {
    return password.strip().toLowerCase(Locale.ROOT);
  }
}
