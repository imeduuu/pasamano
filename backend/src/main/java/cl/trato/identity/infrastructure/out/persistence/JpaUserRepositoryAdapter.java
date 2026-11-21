package cl.trato.identity.infrastructure.out.persistence;

import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.PasswordHash;
import cl.trato.identity.domain.Profile;
import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA del puerto {@link UserRepository}. El mapeo es manual porque el agregado se
 * reconstruye con su fábrica {@code rehydrate}.
 */
@Component
class JpaUserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository jpa;

  JpaUserRepositoryAdapter(UserJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<User> findById(UserId id) {
    return jpa.findById(id.value()).map(JpaUserRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    return jpa.findByEmail(email.value()).map(JpaUserRepositoryAdapter::toDomain);
  }

  @Override
  public boolean existsByEmail(Email email) {
    return jpa.existsByEmail(email.value());
  }

  @Override
  public User save(User user) {
    return toDomain(jpa.saveAndFlush(toEntity(user)));
  }

  static User toDomain(UserJpaEntity entity) {
    return User.rehydrate(
        new UserId(entity.getId()),
        new Email(entity.getEmail()),
        new PasswordHash(entity.getPasswordHash()),
        entity.getStatus(),
        entity.getRoles().isEmpty()
            ? EnumSet.noneOf(Role.class)
            : EnumSet.copyOf(entity.getRoles()),
        new Profile(
            new DisplayName(entity.getName()),
            entity.getRegion(),
            entity.getCommune(),
            entity.getPhotoUrl()),
        entity.getFailedLoginAttempts(),
        entity.getLockedUntil(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }

  static UserJpaEntity toEntity(User user) {
    UserJpaEntity entity = new UserJpaEntity();
    entity.setId(user.id().value());
    entity.setEmail(user.email().value());
    entity.setPasswordHash(user.passwordHash().value());
    entity.setName(user.profile().name().value());
    entity.setStatus(user.status());
    entity.setRoles(
        user.roles().isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(user.roles()));
    entity.setRegion(user.profile().region());
    entity.setCommune(user.profile().commune());
    entity.setPhotoUrl(user.profile().photoUrl());
    entity.setFailedLoginAttempts(user.failedLoginAttempts());
    entity.setLockedUntil(user.lockedUntil());
    entity.setCreatedAt(user.createdAt());
    entity.setUpdatedAt(user.updatedAt());
    entity.setVersion(user.version());
    return entity;
  }
}
