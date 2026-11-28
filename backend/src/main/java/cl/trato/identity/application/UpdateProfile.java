package cl.trato.identity.application;

import cl.trato.identity.application.port.ProfileLocationValidator;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.DisplayName;
import cl.trato.identity.domain.Profile;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.exception.UserNotFoundException;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/** RF-USR-01: el usuario edita su propio perfil (nombre visible, foto, región y comuna). */
public class UpdateProfile {

  private final UserRepository users;
  private final SellerProfileRepository sellerProfiles;
  private final ProfileLocationValidator locations;
  private final Clock clock;

  public UpdateProfile(
      UserRepository users,
      SellerProfileRepository sellerProfiles,
      ProfileLocationValidator locations,
      Clock clock) {
    this.users = users;
    this.sellerProfiles = sellerProfiles;
    this.locations = locations;
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
    if (updated.region() != null && !locations.isValid(updated.region(), updated.commune())) {
      throw new DomainValidationException(
          "INVALID_COMMUNE", "La comuna no pertenece a la región seleccionada.");
    }
    user.updateProfile(updated, clock.instant());
    return UserProfileView.of(
        users.save(user), sellerProfiles.findByUserId(command.userId()).isPresent());
  }
}
