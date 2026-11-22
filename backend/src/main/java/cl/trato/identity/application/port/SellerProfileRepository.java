package cl.trato.identity.application.port;

import cl.trato.identity.domain.SellerProfile;
import cl.trato.identity.domain.UserId;
import java.util.Optional;

/** Puerto de persistencia del perfil vendedor (RF-USR-02). */
public interface SellerProfileRepository {

  Optional<SellerProfile> findByUserId(UserId id);

  boolean existsByNormalizedStoreName(String normalizedStoreName);

  SellerProfile save(SellerProfile profile);
}
