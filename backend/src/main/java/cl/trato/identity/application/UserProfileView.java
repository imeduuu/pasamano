package cl.trato.identity.application;

import cl.trato.identity.domain.Role;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Vista del perfil propio.
 *
 * @param id identificador
 * @param email correo
 * @param name nombre visible
 * @param region región
 * @param commune comuna
 * @param photoUrl URL de la foto
 * @param status estado de la cuenta
 * @param roles roles
 * @param createdAt fecha de registro (UTC)
 */
public record UserProfileView(
    String id,
    String email,
    String name,
    String region,
    String commune,
    String photoUrl,
    UserStatus status,
    Set<Role> roles,
    Instant createdAt) {

  static UserProfileView of(User user) {
    return new UserProfileView(
        user.id().toString(),
        user.email().value(),
        user.profile().name().value(),
        user.profile().region(),
        user.profile().commune(),
        user.profile().photoUrl(),
        user.status(),
        Set.copyOf(user.roles()),
        user.createdAt());
  }
}
