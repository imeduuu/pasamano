package cl.trato.cart.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fila JPA del carrito, separada del agregado de dominio. */
@Entity
@Table(name = "carts")
public class CartJpaEntity {
  @Id private UUID buyerId;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CartItemJpaEntity> items = new ArrayList<>();

  public UUID getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(UUID buyerId) {
    this.buyerId = buyerId;
  }

  public List<CartItemJpaEntity> getItems() {
    return items;
  }

  public void setItems(List<CartItemJpaEntity> items) {
    this.items = items;
  }
}
