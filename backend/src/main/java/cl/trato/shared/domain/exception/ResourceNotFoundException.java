package cl.trato.shared.domain.exception;

/** El recurso solicitado no existe. */
public class ResourceNotFoundException extends DomainException {

  public ResourceNotFoundException(String code, String message) {
    super(code, ErrorCategory.NOT_FOUND, message);
  }
}
