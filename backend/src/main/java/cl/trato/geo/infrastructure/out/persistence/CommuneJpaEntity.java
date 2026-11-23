package cl.trato.geo.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de solo lectura de la tabla {@code communes} (datos de referencia de la migración V10). */
@Entity
@Table(name = "communes")
public class CommuneJpaEntity {
  @Id
  @Column(length = 5)
  private String code;

  @Column(nullable = false, length = 60)
  private String name;

  @Column(name = "region_code", nullable = false, length = 2)
  private String regionCode;

  protected CommuneJpaEntity() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getRegionCode() {
    return regionCode;
  }
}
