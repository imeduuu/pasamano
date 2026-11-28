package cl.trato.cart.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Snapshot persistido de una publicación añadida al carrito. */
@Entity
@Table(name = "cart_items")
public class CartItemJpaEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_id", nullable = false)
  private CartJpaEntity cart;

  @Column(name = "listing_id", nullable = false)
  private UUID listingId;

  @Column(nullable = false, length = 80)
  private String title;

  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private long unitPrice;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public CartJpaEntity getCart() {
    return cart;
  }

  public void setCart(CartJpaEntity cart) {
    this.cart = cart;
  }

  public UUID getListingId() {
    return listingId;
  }

  public void setListingId(UUID listingId) {
    this.listingId = listingId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public UUID getSellerId() {
    return sellerId;
  }

  public void setSellerId(UUID sellerId) {
    this.sellerId = sellerId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public long getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(long unitPrice) {
    this.unitPrice = unitPrice;
  }
}
