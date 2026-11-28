package cl.trato.cart.infrastructure.out.persistence;

import cl.trato.cart.application.port.CartRepository;
import cl.trato.cart.domain.Cart;
import cl.trato.cart.domain.CartItem;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA de RF-CAR-05. */
@Component
class JpaCartRepositoryAdapter implements CartRepository {
  private final CartJpaRepository jpa;

  JpaCartRepositoryAdapter(CartJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Cart> findByBuyerId(UserId buyerId) {
    return jpa.findById(buyerId.value()).map(this::toDomain);
  }

  @Override
  @Transactional
  public Cart save(Cart cart) {
    CartJpaEntity entity = jpa.findById(cart.buyerId().value()).orElseGet(CartJpaEntity::new);
    entity.setBuyerId(cart.buyerId().value());
    entity.getItems().clear();
    for (CartItem item : cart.items()) {
      CartItemJpaEntity row = new CartItemJpaEntity();
      row.setId(UUID.randomUUID());
      row.setCart(entity);
      row.setListingId(item.listingId());
      row.setTitle(item.title());
      row.setSellerId(item.sellerId().value());
      row.setQuantity(item.quantity());
      row.setUnitPrice(item.unitPrice().amount());
      entity.getItems().add(row);
    }
    return toDomain(jpa.save(entity));
  }

  private Cart toDomain(CartJpaEntity entity) {
    return Cart.rehydrate(
        new UserId(entity.getBuyerId()),
        entity.getItems().stream()
            .map(item ->
                new CartItem(
                    item.getListingId(),
                    item.getTitle(),
                    new UserId(item.getSellerId()),
                    item.getQuantity(),
                    Money.price(item.getUnitPrice())))
            .toList());
  }
}
