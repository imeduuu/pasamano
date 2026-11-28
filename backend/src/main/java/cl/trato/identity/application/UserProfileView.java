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
 * @param region código CUT de región
 * @param commune código CUT de comuna
 * @param photoUrl URL de la foto
 * @param status estado de la cuenta
 * @param roles roles
 * @param seller vendedor
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
    boolean seller,
    Instant createdAt) {

  static UserProfileView of(User user, boolean seller) {
    return new UserProfileView(
        user.id().toString(),
        user.email().value(),
        user.profile().name().value(),
        user.profile().region(),
        user.profile().commune(),
        user.profile().photoUrl(),
        user.status(),
        Set.copyOf(user.roles()),
        seller,
        user.createdAt());
  }
}
