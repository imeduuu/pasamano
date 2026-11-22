package cl.trato.catalog.infrastructure.out.persistence;

import cl.trato.catalog.domain.ListingCondition;
import cl.trato.catalog.domain.ListingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fila JPA de una publicación; permanece separada del agregado de dominio. */
@Entity
@Table(name = "listings")
public class ListingJpaEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 80)
  private String title;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(nullable = false)
  private Long price;

  @Column(name = "stock_total", nullable = false)
  private int stockTotal;

  @Column(name = "stock_reserved", nullable = false)
  private int stockReserved;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ListingCondition condition;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Column(nullable = false, length = 60)
  private String region;

  @Column(nullable = false, length = 60)
  private String commune;

  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ListingStatus status;

  @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ListingImageJpaEntity> images = new ArrayList<>();

  @Version private Long version;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getPrice() {
    return price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public int getStockTotal() {
    return stockTotal;
  }

  public void setStockTotal(int stockTotal) {
    this.stockTotal = stockTotal;
  }

  public int getStockReserved() {
    return stockReserved;
  }

  public void setStockReserved(int stockReserved) {
    this.stockReserved = stockReserved;
  }

  public ListingCondition getCondition() {
    return condition;
  }

  public void setCondition(ListingCondition condition) {
    this.condition = condition;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(UUID categoryId) {
    this.categoryId = categoryId;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getCommune() {
    return commune;
  }

  public void setCommune(String commune) {
    this.commune = commune;
  }

  public UUID getSellerId() {
    return sellerId;
  }

  public void setSellerId(UUID sellerId) {
    this.sellerId = sellerId;
  }

  public ListingStatus getStatus() {
    return status;
  }

  public void setStatus(ListingStatus status) {
    this.status = status;
  }

  public List<ListingImageJpaEntity> getImages() {
    return images;
  }

  public void setImages(List<ListingImageJpaEntity> images) {
    this.images = images;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
