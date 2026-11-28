package cl.trato.identity.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.identity.application.port.ConsentRepository;
import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.Consent;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.LegalVersions;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.Profile;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.UserStatus;
import cl.trato.support.IntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Adaptadores de persistencia de identidad contra PostgreSQL 16 real (Testcontainers). */
@IntegrationTest
class IdentityPersistenceIT {

  static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

  @Autowired UserRepository users;
  @Autowired RefreshTokenRepository refreshTokens;
  @Autowired OneTimeTokenRepository oneTimeTokens;
  @Autowired ConsentRepository consents;
  @Autowired cl.trato.identity.application.port.SellerProfileRepository sellerProfiles;
  @Autowired JdbcTemplate jdbc;

  User newUser() {
    User user =
        User.register(
            UserId.newId(),
            new Email("p" + UUID.randomUUID().toString().substring(0, 10) + "@ejemplo.cl"),
            new PasswordHash("$2a$12$hash"),
            new DisplayName("Persona Prueba"),
            NOW);
    user.pullDomainEvents();
    return user;
  }

  static String hash() {
    return UUID.randomUUID().toString().replace("-", "") + "0123456789abcdef0123456789abcdef";
  }

  @Test
  void saves_and_loads_a_user_with_roles_profile_and_lockout_state() {
    User user = newUser();
    user.verifyEmail(NOW);
    user.updateProfile(new Profile(new DisplayName("Ana"), "08", "08101", null), NOW);
    for (int i = 0; i < 2; i++) {
      user.registerFailedLogin(NOW);
    }

    users.save(user);
    User loaded = users.findById(user.id()).orElseThrow();

    assertThat(loaded.email()).isEqualTo(user.email());
    assertThat(loaded.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(loaded.roles()).containsExactly(Role.USER);
    assertThat(loaded.profile().region()).isEqualTo("08");
    assertThat(loaded.failedLoginAttempts()).isEqualTo(2);
    assertThat(loaded.createdAt()).isEqualTo(NOW);
    assertThat(loaded.version()).isZero();
    assertThat(users.findByEmail(user.email())).isPresent();
    assertThat(users.existsByEmail(user.email())).isTrue();
    assertThat(users.existsByEmail(new Email("nadie-" + UUID.randomUUID() + "@x.cl"))).isFalse();
  }

  @Test
  void the_email_is_unique() {
    User first = newUser();
    users.save(first);
    User duplicate =
        User.register(
            UserId.newId(),
            first.email(),
            new PasswordHash("$2a$12$x"),
            new DisplayName("Otra"),
            NOW);

    assertThatThrownBy(() -> users.save(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrent_modifications_are_detected_by_version() {
    User user = users.save(newUser());
    User copyA = users.findById(user.id()).orElseThrow();
    User copyB = users.findById(user.id()).orElseThrow();

    copyA.registerFailedLogin(NOW);
    users.save(copyA);
    copyB.registerFailedLogin(NOW);

    assertThatThrownBy(() -> users.save(copyB))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  @Test
  void refresh_tokens_are_found_by_hash_and_revoked_by_family_or_user() {
    User user = users.save(newUser());
    RefreshToken first = RefreshToken.startFamily(user.id(), hash(), NOW);
    RefreshToken.Rotation rotation = first.rotate(hash(), NOW);
    RefreshToken otherFamily = RefreshToken.startFamily(user.id(), hash(), NOW);
    refreshTokens.save(rotation.used());
    refreshTokens.save(rotation.next());
    refreshTokens.save(otherFamily);

    assertThat(refreshTokens.findByHash(first.tokenHash()))
        .hasValueSatisfying(t -> assertThat(t.usedAt()).isEqualTo(NOW));

    refreshTokens.revokeFamily(first.familyId(), NOW);
    assertThat(refreshTokens.findByHash(rotation.next().tokenHash()).orElseThrow().revokedAt())
        .isEqualTo(NOW);
    assertThat(refreshTokens.findByHash(otherFamily.tokenHash()).orElseThrow().revokedAt())
        .isNull();

    refreshTokens.revokeAllForUser(user.id(), NOW);
    assertThat(refreshTokens.findByHash(otherFamily.tokenHash()).orElseThrow().revokedAt())
        .isEqualTo(NOW);
  }

  @Test
  void one_time_tokens_are_found_by_hash_and_purpose_and_can_be_invalidated() {
    User user = users.save(newUser());
    OneTimeToken verification =
        OneTimeToken.issue(user.id(), TokenPurpose.EMAIL_VERIFICATION, hash(), NOW);
    OneTimeToken reset = OneTimeToken.issue(user.id(), TokenPurpose.PASSWORD_RESET, hash(), NOW);
    oneTimeTokens.save(verification);
    oneTimeTokens.save(reset);

    assertThat(oneTimeTokens.findByHash(verification.tokenHash(), TokenPurpose.EMAIL_VERIFICATION))
        .hasValueSatisfying(t -> assertThat(t.expiresAt()).isEqualTo(verification.expiresAt()));
    assertThat(oneTimeTokens.findByHash(verification.tokenHash(), TokenPurpose.PASSWORD_RESET))
        .isEmpty();

    oneTimeTokens.invalidateActive(user.id(), TokenPurpose.EMAIL_VERIFICATION, NOW);

    assertThat(
            oneTimeTokens
                .findByHash(verification.tokenHash(), TokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow()
                .usedAt())
        .isEqualTo(NOW);
    assertThat(
            oneTimeTokens.findByHash(reset.tokenHash(), TokenPurpose.PASSWORD_RESET).orElseThrow())
        .satisfies(t -> assertThat(t.usedAt()).isNull());

    oneTimeTokens.save(reset.consume(NOW));
    assertThat(
            oneTimeTokens
                .findByHash(reset.tokenHash(), TokenPurpose.PASSWORD_RESET)
                .orElseThrow()
                .usedAt())
        .isEqualTo(NOW);
  }

  @Test
  void consents_are_recorded_with_versions_and_date() {
    User user = users.save(newUser());

    consents.save(Consent.accept(user.id(), new LegalVersions("2026-11-20", "2026-11-21"), NOW));

    var row =
        jdbc.queryForMap(
            "select terms_version, privacy_version, accepted_at from consents where user_id = ?",
            user.id().value());
    assertThat(row)
        .containsEntry("terms_version", "2026-11-20")
        .containsEntry("privacy_version", "2026-11-21");
  }

  @Test
  void seller_profiles_round_trip_and_store_names_are_unique_without_case_sensitivity() {
    User first = users.save(newUser());
    User second = users.save(newUser());
    SellerProfile profile = SellerProfile.activate(first.id(), "Tienda Única", "2026-11-20", NOW);
    sellerProfiles.save(profile);

    assertThat(sellerProfiles.findByUserId(first.id())).contains(profile);
    assertThat(sellerProfiles.existsByNormalizedStoreName("tienda única")).isTrue();
    SellerProfile duplicate =
        SellerProfile.activate(second.id(), "TIENDA ÚNICA", "2026-11-20", NOW);
    assertThatThrownBy(() -> sellerProfiles.save(duplicate))
        .isInstanceOf(cl.trato.shared.domain.exception.ConflictException.class);
  }

  @Test
  void a_user_can_be_rehydrated_with_admin_role() {
    User admin =
        User.rehydrate(
            UserId.newId(),
            new Email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@ejemplo.cl"),
            new PasswordHash("$2a$12$x"),
            UserStatus.ACTIVE,
            Set.of(Role.USER, Role.ADMIN),
            Profile.withName(new DisplayName("Moderación")),
            0,
            null,
            NOW,
            NOW,
            null);

    users.save(admin);

    assertThat(users.findById(admin.id()).orElseThrow().isAdmin()).isTrue();
  }
}
