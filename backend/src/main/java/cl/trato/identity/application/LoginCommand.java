package cl.trato.identity.application;

/**
 * Credenciales de inicio de sesión (CU-02).
 *
 * @param email correo
 * @param password contraseña en claro
 */
public record LoginCommand(String email, String password) {

  @Override
  public String toString() {
    return "LoginCommand[***]";
  }
}
