package cl.trato.identity.application;

import cl.trato.identity.application.port.AccessTokenIssuer;
import cl.trato.identity.application.port.ConsentRepository;
import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.ProfileLocationValidator;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.Consent;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.LegalVersions;
import cl.trato.identity.domain.OneTimeToken;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.PasswordPolicy;
import cl.trato.identity.domain.RefreshToken;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.TokenPurpose;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.application.DomainEventPublisher;
import cl.trato.shared.domain.event.DomainEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Puertos falsos en memoria para probar los casos de uso sin Spring ni base de datos. */
final class IdentityFakes {

  static final Instant START = Instant.parse("2026-12-07T12:00:00Z");
  static final LegalVersions LEGAL = new LegalVersions("2026-11-20", "2026-11-20");
  static final String GOOD_PASSWORD = "caballo-bateria-grapa";

  final MutableClock clock = new MutableClock(START);
  final InMemoryUsers users = new InMemoryUsers();
  final InMemorySellerProfiles sellerProfiles = new InMemorySellerProfiles();
  final ProfileLocationValidator locations =
      (region, commune) ->
          (region.equals("05") && commune.equals("05109"))
              || (region.equals("08") && commune.equals("08101"));
  final InMemoryConsents consents = new InMemoryConsents();
  final InMemoryOneTimeTokens oneTimeTokens = new InMemoryOneTimeTokens();
  final InMemoryRefreshTokens refreshTokens = new InMemoryRefreshTokens();
  final FakePasswordHasher hasher = new FakePasswordHasher();
  final SequentialTokenGenerator tokenGenerator = new SequentialTokenGenerator();
  final FakeAccessTokenIssuer accessTokens = new FakeAccessTokenIssuer();
  final RecordingEvents events = new RecordingEvents();
  final PasswordPolicy policy = new PasswordPolicy(Set.of("password123", "qwertyuiop"));

  final OneTimeTokenIssuer tokenIssuer = new OneTimeTokenIssuer(oneTimeTokens, tokenGenerator);
  final SessionIssuer sessionIssuer =
      new SessionIssuer(refreshTokens, tokenGenerator, accessTokens);

  RegisterUser registerUser() {
    return new RegisterUser(users, consents, policy, hasher, tokenIssuer, events, LEGAL, clock);
  }

  VerifyEmail verifyEmail() {
    return new VerifyEmail(users, oneTimeTokens, tokenGenerator, clock);
  }

  ResendVerification resendVerification() {
    return new ResendVerification(users, tokenIssuer, events, clock);
  }

  Login login() {
    return new Login(users, hasher, sessionIssuer, clock);
  }

  RefreshSession refreshSession() {
    return new RefreshSession(users, refreshTokens, tokenGenerator, accessTokens, clock);
  }

  Logout logout() {
    return new Logout(refreshTokens, tokenGenerator, clock);
  }

  RequestPasswordReset requestPasswordReset() {
    return new RequestPasswordReset(users, tokenIssuer, events, clock);
  }

  ResetPassword resetPassword() {
    return new ResetPassword(
        users, oneTimeTokens, refreshTokens, policy, hasher, tokenGenerator, clock);
  }

  GetProfile getProfile() {
    return new GetProfile(users, sellerProfiles);
  }

  private static final class InMemorySellerProfiles implements SellerProfileRepository {
    private final Map<UserId, SellerProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<SellerProfile> findByUserId(UserId id) {
      return Optional.ofNullable(profiles.get(id));
    }

    @Override
    public boolean existsByNormalizedStoreName(String normalizedStoreName) {
      return profiles.values().stream()
          .anyMatch(profile -> profile.normalizedStoreName().equals(normalizedStoreName));
    }

    @Override
    public SellerProfile save(SellerProfile profile) {
      profiles.put(profile.userId(), profile);
      return profile;
    }
  }

  UpdateProfile updateProfile() {
    return new UpdateProfile(users, sellerProfiles, locations, clock);
  }

  ChangePassword changePassword() {
    return new ChangePassword(users, refreshTokens, policy, hasher, clock);
  }

  /** Crea y guarda un usuario ACTIVO con {@link #GOOD_PASSWORD}. */
  User activeUser(String email) {
    User user =
        User.register(
            UserId.newId(),
            new Email(email),
            hasher.hash(GOOD_PASSWORD),
            new DisplayName("Usuaria Prueba"),
            clock.instant());
    user.verifyEmail(clock.instant());
    user.pullDomainEvents();
    return users.save(user);
  }

  User pendingUser(String email) {
    User user =
        User.register(
            UserId.newId(),
            new Email(email),
            hasher.hash(GOOD_PASSWORD),
            new DisplayName("Usuaria Pendiente"),
            clock.instant());
    user.pullDomainEvents();
    return users.save(user);
  }

  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  static final class InMemoryUsers implements UserRepository {
    final Map<UserId, User> byId = new ConcurrentHashMap<>();
    int saves;

    @Override
    public Optional<User> findById(UserId id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return byId.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
      return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
      saves++;
      byId.put(user.id(), user);
      return user;
    }
  }

  static final class InMemoryConsents implements ConsentRepository {
    final List<Consent> saved = new ArrayList<>();

    @Override
    public void save(Consent consent) {
      saved.add(consent);
    }
  }

  static final class InMemoryOneTimeTokens implements OneTimeTokenRepository {
    final Map<UUID, OneTimeToken> byId = new ConcurrentHashMap<>();

    @Override
    public void save(OneTimeToken token) {
      byId.put(token.id(), token);
    }

    @Override
    public Optional<OneTimeToken> findByHash(String tokenHash, TokenPurpose purpose) {
      return byId.values().stream()
          .filter(t -> t.tokenHash().equals(tokenHash) && t.purpose() == purpose)
          .findFirst();
    }

    @Override
    public void invalidateActive(UserId userId, TokenPurpose purpose, Instant now) {
      byId.replaceAll((id, t) ->
          t.userId().equals(userId) && t.purpose() == purpose && t.usedAt() == null
              ? new OneTimeToken(
                  t.id(), t.userId(), t.purpose(), t.tokenHash(), t.expiresAt(), now, t.createdAt())
              : t);
    }

    List<OneTimeToken> of(UserId userId, TokenPurpose purpose) {
      return byId.values().stream()
          .filter(t -> t.userId().equals(userId) && t.purpose() == purpose)
          .toList();
    }
  }

  static final class InMemoryRefreshTokens implements RefreshTokenRepository {
    final Map<UUID, RefreshToken> byId = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshToken token) {
      byId.put(token.id(), token);
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
      return byId.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
      byId.replaceAll((id, t) -> t.familyId().equals(familyId) ? t.revoke(now) : t);
    }

    @Override
    public void revokeAllForUser(UserId userId, Instant now) {
      byId.replaceAll((id, t) -> t.userId().equals(userId) ? t.revoke(now) : t);
    }

    List<RefreshToken> family(UUID familyId) {
      return byId.values().stream().filter(t -> t.familyId().equals(familyId)).toList();
    }

    List<RefreshToken> ofUser(UserId userId) {
      return byId.values().stream().filter(t -> t.userId().equals(userId)).toList();
    }
  }

  static final class FakePasswordHasher implements PasswordHasher {
    int dummyChecks;

    @Override
    public PasswordHash hash(String rawPassword) {
      return new PasswordHash("hashed:" + rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
      return hash.value().equals("hashed:" + rawPassword);
    }

    @Override
    public void matchesDummy(String rawPassword) {
      dummyChecks++;
    }
  }

  static final class SequentialTokenGenerator implements TokenGenerator {
    private final AtomicInteger counter = new AtomicInteger();
    String last;

    @Override
    public String generate() {
      last = "token-" + counter.incrementAndGet();
      return last;
    }

    @Override
    public String hash(String rawToken) {
      return "sha(" + rawToken + ")";
    }
  }

  static final class FakeAccessTokenIssuer implements AccessTokenIssuer {
    @Override
    public IssuedAccessToken issue(User user, Instant now) {
      return new IssuedAccessToken("jwt-for-" + user.id(), now.plus(Duration.ofMinutes(15)));
    }
  }

  static final class RecordingEvents implements DomainEventPublisher {
    final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
      published.addAll(events);
    }

    <T extends DomainEvent> List<T> ofType(Class<T> type) {
      return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
  }
}
