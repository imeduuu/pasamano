package cl.trato.identity.infrastructure.out.security;

import cl.trato.identity.application.port.AccessTokenIssuer;
import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.User;
import cl.trato.identity.infrastructure.config.IdentityProperties;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el JWT de acceso firmado con HS256 (ADR-002). Solo lleva el identificador y los roles:
 * ningún dato personal (minimización, RNF-LEG-01).
 */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  public static final String ROLES_CLAIM = "roles";

  private final JwtEncoder encoder;
  private final IdentityProperties.Jwt properties;

  public JwtAccessTokenIssuer(JwtEncoder encoder, IdentityProperties properties) {
    this.encoder = encoder;
    this.properties = properties.jwt();
  }

  @Override
  public IssuedAccessToken issue(User user, Instant now) {
    Instant expiresAt = now.plus(properties.accessTokenTtl());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(user.id().toString())
            .issuedAt(now)
            .notBefore(now)
            .expiresAt(expiresAt)
            .claim(ROLES_CLAIM, user.roles().stream().map(Role::name).sorted().toList())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, expiresAt);
  }
}
