package cl.trato.identity.application;

import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** RF-USR-01: el usuario ve su propio perfil. */
public class GetProfile {

  private final UserRepository users;

  public GetProfile(UserRepository users) {
    this.users = users;
  }

  @Transactional(readOnly = true)
  public UserProfileView execute(UserId userId) {
    return users.findById(userId).map(UserProfileView::of).orElseThrow(UserNotFoundException::new);
  }
}
