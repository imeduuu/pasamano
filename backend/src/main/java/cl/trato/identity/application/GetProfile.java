package cl.trato.identity.application;

import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** RF-USR-01: el usuario ve su propio perfil. */
public class GetProfile {

  private final UserRepository users;
  private final SellerProfileRepository sellerProfiles;

  public GetProfile(UserRepository users, SellerProfileRepository sellerProfiles) {
    this.users = users;
    this.sellerProfiles = sellerProfiles;
  }

  @Transactional(readOnly = true)
  public UserProfileView execute(UserId userId) {
    return users
        .findById(userId)
        .map(user -> UserProfileView.of(user, sellerProfiles.findByUserId(userId).isPresent()))
        .orElseThrow(UserNotFoundException::new);
  }
}
