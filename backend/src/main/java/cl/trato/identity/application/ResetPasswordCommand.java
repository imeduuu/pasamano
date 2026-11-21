package cl.trato.identity.application;

/**
 * Restablecimiento de contraseña con el enlace recibido (CU-16).
 *
 * @param token token en claro del enlace
 * @param newPassword nueva contraseña
 */
public record ResetPasswordCommand(String token, String newPassword) {

  @Override
  public String toString() {
    return "ResetPasswordCommand[***]";
  }
}
