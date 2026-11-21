package cl.trato.identity.infrastructure.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.infrastructure.config.IdentityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Adaptadores de seguridad: BCrypt factor 12, tokens opacos y JWT de 15 minutos. */
class SecurityAdaptersTest {

  @Test
  void bcrypt_hashes_with_cost_12_and_verifies() {
    BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    PasswordHash hash = hasher.hash("caballo-bateria-grapa");

    assertThat(hash.value()).startsWith("$2a$12$").doesNotContain("caballo");
    assertThat(hasher.matches("caballo-bateria-grapa", hash)).isTrue();
    assertThat(hasher.matches("otra", hash)).isFalse();
    assertThat(hasher.matches(null, hash)).isFalse();
  }

  @Test
  void bcrypt_never_matches_passwords_longer_than_72_bytes() {
    BCryptPasswordHasher hasher = new BCryptPasswordHasher(4);
    PasswordHash hash = hasher.hash("a".repeat(72));

    assertThat(hasher.matches("a".repeat(80), hash)).isFalse();
  }

  @Test
  void the_dummy_check_runs_without_failing() {
    BCryptPasswordHasher hasher = new BCryptPasswordHasher(4);

    hasher.matchesDummy("lo-que-sea");
    hasher.matchesDummy(null);

    assertThat(hasher).isNotNull();
  }

  @Test
  void generated_tokens_are_random_url_safe_and_256_bits() {
    SecureTokenGenerator generator = new SecureTokenGenerator();
    Set<String> tokens = new HashSet<>();

    for (int i = 0; i < 100; i++) {
      tokens.add(generator.generate());
    }

    assertThat(tokens).hasSize(100).allMatch(t -> t.matches("^[A-Za-z0-9_-]{43}$"));
  }

  @Test
  void token_hashes_are_deterministic_sha256_hex() {
    SecureTokenGenerator generator = new SecureTokenGenerator();

    assertThat(generator.hash("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        .isEqualTo(generator.hash("abc"));
  }

  @Test
  void issues_a_signed_jwt_with_subject_roles_and_15_minute_expiry() {
    IdentityProperties properties =
        new IdentityProperties(
            new IdentityProperties.Jwt(
                "clave-de-pruebas-unitarias-con-mas-de-32-bytes", "trato", Duration.ofMinutes(15)),
            null,
            null,
            null);
    SecretKey key =
        new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtAccessTokenIssuer issuer =
        new JwtAccessTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties);
    JwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    User user =
        User.register(
            UserId.newId(),
            new Email("ana@ejemplo.cl"),
            new PasswordHash("$2a$12$x"),
            new DisplayName("Ana"),
            Instant.now());
    Instant now = Instant.now();

    var issued = issuer.issue(user, now);
    Jwt jwt = decoder.decode(issued.token());

    assertThat(issued.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("trato");
    assertThat(jwt.getClaims()).doesNotContainKeys("email", "name");
  }
}
