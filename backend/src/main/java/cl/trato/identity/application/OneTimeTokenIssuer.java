package cl.trato.identity.application;

import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.UserId;
import java.time.Instant;

/**
 * Emite un token de un solo uso: invalida los vigentes del mismo propósito, guarda solo el hash y
 * devuelve el valor en claro para enviarlo por correo.
 */
public class OneTimeTokenIssuer {

  private final OneTimeTokenRepository tokens;
  private final TokenGenerator generator;

  public OneTimeTokenIssuer(OneTimeTokenRepository tokens, TokenGenerator generator) {
    this.tokens = tokens;
    this.generator = generator;
  }

  public Issued issue(UserId userId, TokenPurpose purpose, Instant now) {
    tokens.invalidateActive(userId, purpose, now);
    String raw = generator.generate();
    OneTimeToken token = OneTimeToken.issue(userId, purpose, generator.hash(raw), now);
    tokens.save(token);
    return new Issued(raw, token.expiresAt());
  }

  /**
   * Token emitido.
   *
   * @param rawToken valor en claro (solo para el correo)
   * @param expiresAt vencimiento
   */
  public record Issued(String rawToken, Instant expiresAt) {}
}
