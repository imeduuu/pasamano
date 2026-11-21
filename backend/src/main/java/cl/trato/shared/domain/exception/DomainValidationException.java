package cl.trato.shared.domain.exception;

/** Datos que no cumplen las invariantes de un value object o entidad. */
public class DomainValidationException extends DomainException {

  public DomainValidationException(String code, String message) {
    super(code, ErrorCategory.INVALID_INPUT, message);
  }
}
