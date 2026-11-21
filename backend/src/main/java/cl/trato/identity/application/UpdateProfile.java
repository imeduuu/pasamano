package cl.trato.identity.application;

import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Profile;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.UserNotFoundException;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/** RF-USR-01: el usuario edita su propio perfil (nombre visible, foto, región y comuna). */
public class UpdateProfile {

  private final UserRepository users;
  private final Clock clock;

  public UpdateProfile(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public UserProfileView execute(UpdateProfileCommand command) {
    User user = users.findById(command.userId()).orElseThrow(UserNotFoundException::new);
    Profile current = user.profile();
    Profile updated =
        new Profile(
            command.name() == null ? current.name() : new DisplayName(command.name()),
            command.region() == null ? current.region() : command.region(),
            command.commune() == null ? current.commune() : command.commune(),
            command.photoUrl() == null ? current.photoUrl() : command.photoUrl());
    user.updateProfile(updated, clock.instant());
    return UserProfileView.of(users.save(user));
  }
}
