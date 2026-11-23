package cl.trato.geo.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de solo lectura de la tabla {@code regions} (datos de referencia de la migración V10). */
@Entity
@Table(name = "regions")
public class RegionJpaEntity {
  @Id
  @Column(length = 2)
  private String code;

  @Column(nullable = false, length = 60)
  private String name;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected RegionJpaEntity() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}
