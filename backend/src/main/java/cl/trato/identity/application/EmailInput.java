package cl.trato.identity.application;

import cl.trato.identity.domain.Email;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.Optional;

/** Interpreta un correo ingresado sin lanzar error, para flujos que no deben revelar nada. */
final class EmailInput {

  private EmailInput() {}

  static Optional<Email> tryParse(String rawEmail) {
    try {
      return Optional.of(new Email(rawEmail));
    } catch (DomainValidationException e) {
      return Optional.empty();
    }
  }
}
