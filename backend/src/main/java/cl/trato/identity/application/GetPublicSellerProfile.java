package cl.trato.identity.application;

import cl.trato.identity.application.port.ActiveListingCount;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** RF-USR-03: consulta la información pública de una tienda. */
public class GetPublicSellerProfile {

  private static final String NEW_SELLER_REPUTATION = "Vendedor nuevo";

  private final SellerProfileRepository sellerProfiles;
  private final ActiveListingCount activeListingCount;

  public GetPublicSellerProfile(
      SellerProfileRepository sellerProfiles, ActiveListingCount activeListingCount) {
    this.sellerProfiles = sellerProfiles;
    this.activeListingCount = activeListingCount;
  }

  @Transactional(readOnly = true)
  public PublicSellerProfileView execute(UserId sellerId) {
    var profile =
        sellerProfiles
            .findByUserId(sellerId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "SELLER_NOT_FOUND", "No se encontró el perfil vendedor."));
    return new PublicSellerProfileView(
        profile.storeName(),
        profile.termsAcceptedAt(),
        NEW_SELLER_REPUTATION,
        0,
        activeListingCount.countForSeller(sellerId));
  }
}
