package cl.trato.identity.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de {@code seller_profiles}. */
@Entity
@Table(name = "seller_profiles")
public class SellerProfileJpaEntity {

  @Id private UUID userId;

  @Column(name = "store_name", nullable = false, length = 40)
  private String storeName;

  @Column(name = "normalized_store_name", nullable = false, length = 40)
  private String normalizedStoreName;

  @Column(name = "terms_version", nullable = false, length = 20)
  private String termsVersion;

  @Column(name = "terms_accepted_at", nullable = false)
  private Instant termsAcceptedAt;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public String getNormalizedStoreName() {
    return normalizedStoreName;
  }

  public void setNormalizedStoreName(String normalizedStoreName) {
    this.normalizedStoreName = normalizedStoreName;
  }

  public String getTermsVersion() {
    return termsVersion;
  }

  public void setTermsVersion(String termsVersion) {
    this.termsVersion = termsVersion;
  }

  public Instant getTermsAcceptedAt() {
    return termsAcceptedAt;
  }

  public void setTermsAcceptedAt(Instant termsAcceptedAt) {
    this.termsAcceptedAt = termsAcceptedAt;
  }
}
