package cl.trato.identity.application.port;

import cl.trato.identity.domain.Email;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import java.util.Optional;

/** Puerto de salida: persistencia de usuarios. */
public interface UserRepository {

  Optional<User> findById(UserId id);

  Optional<User> findByEmail(Email email);

  boolean existsByEmail(Email email);

  /** Guarda el usuario; si la versión quedó obsoleta, falla por concurrencia optimista. */
  User save(User user);
}
