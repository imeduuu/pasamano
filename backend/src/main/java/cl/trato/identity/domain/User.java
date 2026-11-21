package cl.trato.identity.domain;

import cl.trato.identity.domain.event.UserRegistered;
import cl.trato.shared.domain.event.AggregateRoot;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Usuario registrado (SRS §8.5). Agrupa credenciales, estado, roles, perfil y el control de
 * intentos fallidos de inicio de sesión.
 */
public final class User extends AggregateRoot {

  /** RF-AUT-08: intentos fallidos consecutivos que bloquean el inicio de sesión. */
  public static final int MAX_FAILED_ATTEMPTS = 5;

  /** RF-AUT-08: duración del bloqueo. */
  public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

  private final UserId id;
  private final Email email;
  private final Instant createdAt;
  private final Long version;
  private final EnumSet<Role> roles;
  private PasswordHash passwordHash;
  private UserStatus status;
  private Profile profile;
  private int failedLoginAttempts;
  private Instant lockedUntil;
  private Instant updatedAt;

  private User(
      UserId id,
      Email email,
      PasswordHash passwordHash,
      UserStatus status,
      Set<Role> roles,
      Profile profile,
      int failedLoginAttempts,
      Instant lockedUntil,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.email = Objects.requireNonNull(email, "email");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.status = Objects.requireNonNull(status, "status");
    this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    this.profile = Objects.requireNonNull(profile, "profile");
    this.failedLoginAttempts = failedLoginAttempts;
    this.lockedUntil = lockedUntil;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /** CU-01 paso 4: la cuenta nace pendiente de verificación y con el rol USER. */
  public static User register(
      UserId id, Email email, PasswordHash passwordHash, DisplayName name, Instant now) {
    User user =
        new User(
            id,
            email,
            passwordHash,
            UserStatus.PENDING_VERIFICATION,
            EnumSet.of(Role.USER),
            Profile.withName(name),
            0,
            null,
            now,
            now,
            null);
    user.registerEvent(new UserRegistered(id, email, now));
    return user;
  }

  /** Reconstruye un usuario guardado, sin emitir eventos. */
  public static User rehydrate(
      UserId id,
      Email email,
      PasswordHash passwordHash,
      UserStatus status,
      Set<Role> roles,
      Profile profile,
      int failedLoginAttempts,
      Instant lockedUntil,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    return new User(
        id,
        email,
        passwordHash,
        status,
        roles,
        profile,
        failedLoginAttempts,
        lockedUntil,
        createdAt,
        updatedAt,
        version);
  }

  /** CU-01 paso 8: el enlace de verificación activa una cuenta pendiente. */
  public void verifyEmail(Instant now) {
    if (status == UserStatus.PENDING_VERIFICATION) {
      status = UserStatus.ACTIVE;
      updatedAt = now;
    }
  }

  public boolean isLocked(Instant now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  /** CU-02 2a/2b: suma un intento fallido; al quinto consecutivo bloquea por 15 minutos. */
  public void registerFailedLogin(Instant now) {
    failedLoginAttempts++;
    if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
      lockedUntil = now.plus(LOCK_DURATION);
      failedLoginAttempts = 0;
    }
    updatedAt = now;
  }

  /** CU-02 paso 3: un inicio de sesión exitoso reinicia el contador. */
  public void registerSuccessfulLogin() {
    failedLoginAttempts = 0;
    lockedUntil = null;
  }

  public void changePassword(PasswordHash newHash, Instant now) {
    passwordHash = Objects.requireNonNull(newHash, "newHash");
    updatedAt = now;
  }

  public void updateProfile(Profile newProfile, Instant now) {
    profile = Objects.requireNonNull(newProfile, "newProfile");
    updatedAt = now;
  }

  /** CU-19 / RN-17: un usuario suspendido no puede iniciar sesión. */
  public void suspend(Instant now) {
    status = UserStatus.SUSPENDED;
    updatedAt = now;
  }

  public void reactivate(Instant now) {
    if (status == UserStatus.SUSPENDED) {
      status = UserStatus.ACTIVE;
      updatedAt = now;
    }
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public boolean isAdmin() {
    return roles.contains(Role.ADMIN);
  }

  public UserId id() {
    return id;
  }

  public Email email() {
    return email;
  }

  public PasswordHash passwordHash() {
    return passwordHash;
  }

  public UserStatus status() {
    return status;
  }

  public Set<Role> roles() {
    return Collections.unmodifiableSet(roles);
  }

  public Profile profile() {
    return profile;
  }

  public int failedLoginAttempts() {
    return failedLoginAttempts;
  }

  public Instant lockedUntil() {
    return lockedUntil;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  /** Versión para el control de concurrencia optimista; {@code null} si aún no se guarda. */
  public Long version() {
    return version;
  }
}
