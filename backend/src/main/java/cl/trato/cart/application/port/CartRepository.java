package cl.trato.cart.application.port;

import cl.trato.cart.domain.Cart;
import cl.trato.identity.domain.UserId;
import java.util.Optional;

/** Puerto de persistencia del carrito individual por comprador; RF-CAR-05. */
public interface CartRepository {
  Optional<Cart> findByBuyerId(UserId buyerId);

  Cart save(Cart cart);
}
