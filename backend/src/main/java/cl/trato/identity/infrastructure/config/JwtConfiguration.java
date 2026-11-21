package cl.trato.identity.infrastructure.config;

import cl.trato.identity.infrastructure.out.security.JwtAccessTokenIssuer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/** Firma y validación del JWT de acceso con HS256 y la clave de {@code JWT_SECRET} (ADR-002). */
@Configuration
public class JwtConfiguration {

  @Bean
  SecretKey jwtSigningKey(IdentityProperties properties) {
    byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
    return new SecretKeySpec(secret, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
  }

  /** Valida firma, vencimiento (con 60 s de tolerancia) y emisor. */
  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSigningKey, IdentityProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
    return decoder;
  }

  /** Convierte el claim {@code roles} en autoridades {@code ROLE_USER} y {@code ROLE_ADMIN}. */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName(JwtAccessTokenIssuer.ROLES_CLAIM);
    authorities.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }
}
