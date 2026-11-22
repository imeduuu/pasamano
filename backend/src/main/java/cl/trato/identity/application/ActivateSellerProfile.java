package cl.trato.identity.application;

import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.User;
import cl.trato.identity.domain.UserId;
import cl.trato.identity.domain.UserStatus;
import cl.trato.identity.domain.exception.AccountSuspendedException;
import cl.trato.identity.domain.exception.EmailNotVerifiedException;
import cl.trato.identity.domain.exception.UserNotFoundException;
import cl.trato.shared.domain.exception.ConflictException;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/** HU-07 / CU-15: activa la capacidad de venta para una cuenta verificada. */
public class ActivateSellerProfile {

  private final UserRepository users;
  private final SellerProfileRepository sellerProfiles;
  private final Clock clock;
  private final String termsVersion;

  public ActivateSellerProfile(
      UserRepository users,
      SellerProfileRepository sellerProfiles,
      Clock clock,
      String termsVersion) {
    this.users = users;
    this.sellerProfiles = sellerProfiles;
    this.clock = clock;
    this.termsVersion = termsVersion;
  }

  @Transactional
  public SellerProfile execute(UserId userId, String storeName) {
    User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
    if (user.status() == UserStatus.SUSPENDED) {
      throw new AccountSuspendedException();
    }
    if (user.status() != UserStatus.ACTIVE) {
      throw new EmailNotVerifiedException();
    }
    if (sellerProfiles.findByUserId(userId).isPresent()) {
      throw new ConflictException("SELLER_PROFILE_EXISTS", "Ya tienes un perfil vendedor.");
    }
    SellerProfile profile =
        SellerProfile.activate(userId, storeName, termsVersion, clock.instant());
    if (sellerProfiles.existsByNormalizedStoreName(profile.normalizedStoreName())) {
      throw new ConflictException("STORE_NAME_TAKEN", "Ese nombre de tienda ya está en uso.");
    }
    return sellerProfiles.save(profile);
  }
}
