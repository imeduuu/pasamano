package cl.trato.shared.domain.exception;

/** Se infringe una regla de negocio (RN-xx). */
public class BusinessRuleViolationException extends DomainException {

  public BusinessRuleViolationException(String code, String message) {
    super(code, ErrorCategory.BUSINESS_RULE, message);
  }
}
