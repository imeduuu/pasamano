package cl.trato.identity.application.port;

/** Puerto de salida: generación de tokens opacos y su hash para guardarlos. */
public interface TokenGenerator {

  /** Token aleatorio criptográficamente seguro, apto para URL y cookies. */
  String generate();

  /** Hash determinista (SHA-256) del token en claro. */
  String hash(String rawToken);
}
