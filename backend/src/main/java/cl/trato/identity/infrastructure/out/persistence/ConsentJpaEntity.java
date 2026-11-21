package cl.trato.identity.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de la tabla {@code consents}. */
@Entity
@Table(name = "consents")
public class ConsentJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "terms_version", nullable = false, length = 20)
  private String termsVersion;

  @Column(name = "privacy_version", nullable = false, length = 20)
  private String privacyVersion;

  @Column(name = "accepted_at", nullable = false)
  private Instant acceptedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getTermsVersion() {
    return termsVersion;
  }

  public void setTermsVersion(String termsVersion) {
    this.termsVersion = termsVersion;
  }

  public String getPrivacyVersion() {
    return privacyVersion;
  }

  public void setPrivacyVersion(String privacyVersion) {
    this.privacyVersion = privacyVersion;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(Instant acceptedAt) {
    this.acceptedAt = acceptedAt;
  }
}
