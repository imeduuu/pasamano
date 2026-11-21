package cl.trato.identity.domain;

/**
 * Hash de una contraseña (BCrypt). Nunca se imprime en logs (RNF-SEG-01).
 *
 * @param value hash codificado
 */
public record PasswordHash(String value) {

  public PasswordHash {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("El hash de la contraseña es obligatorio");
    }
  }

  @Override
  public String toString() {
    return "PasswordHash[***]";
  }
}
