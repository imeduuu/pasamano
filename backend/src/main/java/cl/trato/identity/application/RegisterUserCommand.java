package cl.trato.identity.application;

/**
 * Datos del formulario de registro (CU-01).
 *
 * @param name nombre visible
 * @param email correo
 * @param password contraseña en claro (nunca se guarda ni se imprime)
 * @param acceptedTerms aceptó términos y política de privacidad
 */
public record RegisterUserCommand(
    String name, String email, String password, boolean acceptedTerms) {

  @Override
  public String toString() {
    return "RegisterUserCommand[name=" + name + "]";
  }
}
