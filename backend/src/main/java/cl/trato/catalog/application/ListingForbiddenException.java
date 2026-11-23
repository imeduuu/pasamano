package cl.trato.catalog.application;

import cl.trato.shared.domain.exception.DomainException;
import cl.trato.shared.domain.exception.ErrorCategory;

/** Operación de publicación prohibida (HU-08/HU-10). */
public class ListingForbiddenException extends DomainException {
  public ListingForbiddenException(String code, String message) {
    super(code, ErrorCategory.FORBIDDEN, message);
  }
}
