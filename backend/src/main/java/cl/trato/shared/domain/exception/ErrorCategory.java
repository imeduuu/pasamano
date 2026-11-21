package cl.trato.shared.domain.exception;

/**
 * Categoría de un error de dominio. Permite que los adaptadores de entrada (HTTP, eventos) lo
 * traduzcan sin que el dominio conozca el protocolo.
 */
public enum ErrorCategory {
  /** Datos de entrada inválidos. */
  INVALID_INPUT,
  /** Se infringe una regla de negocio (RN-xx). */
  BUSINESS_RULE,
  /** El recurso no existe. */
  NOT_FOUND,
  /** El recurso cambió o ya existe (concurrencia, duplicado). */
  CONFLICT,
  /** No se pudo autenticar al usuario. */
  AUTHENTICATION,
  /** El usuario está autenticado pero no puede hacer la operación. */
  FORBIDDEN,
  /** El recurso está bloqueado temporalmente. */
  LOCKED
}
