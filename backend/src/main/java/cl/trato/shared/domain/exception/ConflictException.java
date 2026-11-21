package cl.trato.shared.domain.exception;

/** El recurso ya existe o cambió de forma concurrente. */
public class ConflictException extends DomainException {

  public ConflictException(String code, String message) {
    super(code, ErrorCategory.CONFLICT, message);
  }
}
