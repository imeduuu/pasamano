package cl.trato.catalog.domain;

import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.Money;
import cl.trato.shared.domain.exception.BusinessRuleViolationException;
import cl.trato.shared.domain.exception.DomainValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Publicación de catálogo; aplica RF-PUB-01, RN-01, RN-05, RN-10 y RN-12. */
public final class Listing {

  private static final int MAX_IMAGES = 8;

  private final UUID id;
  private final Long version;
  private final String title;
  private final String description;
  private final Money price;
  private int stockTotal;
  private int stockReserved;
  private final ListingCondition condition;
  private final Category category;
  private final String region;
  private final String commune;
  private final UserId seller;
  private final List<String> images = new ArrayList<>();
  private ListingStatus status = ListingStatus.BORRADOR;

  private Listing(
      String title,
      String description,
      Money price,
      int stockTotal,
      int stockReserved,
      ListingCondition condition,
      Category category,
      String region,
      String commune,
      UserId seller,
      UUID id,
      Long version,
      ListingStatus status,
      List<String> images) {
    if (title == null || title.strip().length() < 10 || title.strip().length() > 80) {
      throw invalid("El título debe tener entre 10 y 80 caracteres.");
    }
    if (description == null || description.length() > 4000) {
      throw invalid("La descripción no puede superar los 4.000 caracteres.");
    }
    if (price == null || !price.isValidPrice()) {
      throw invalid("El precio no cumple RN-01.");
    }
    if (stockTotal < 0 || stockReserved < 0 || stockReserved > stockTotal) {
      throw invalid("El stock reservado debe estar entre 0 y el stock total.");
    }
    if (condition == null || category == null || category.parentId() == null) {
      throw invalid("Debes indicar condición y una categoría hoja.");
    }
    this.region = requiredLocation(region, "región");
    this.commune = requiredLocation(commune, "comuna");
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.title = title.strip();
    this.description = description;
    this.price = price;
    this.stockTotal = stockTotal;
    this.stockReserved = stockReserved;
    this.condition = condition;
    this.category = category;
    this.seller = Objects.requireNonNull(seller, "seller");
    this.status = Objects.requireNonNull(status, "status");
    this.images.addAll(images);
  }

  public static Listing create(
      String title,
      String description,
      Money price,
      int stockTotal,
      int stockReserved,
      ListingCondition condition,
      Category category,
      String region,
      String commune,
      UserId seller) {
    return new Listing(
        title,
        description,
        price,
        stockTotal,
        stockReserved,
        condition,
        category,
        region,
        commune,
        seller,
        UUID.randomUUID(),
        null,
        ListingStatus.BORRADOR,
        List.of());
  }

  /** Reconstruye una publicación persistida sin alterar sus datos ni emitir efectos; RF-PUB-01. */
  public static Listing rehydrate(
      UUID id,
      String title,
      String description,
      Money price,
      int stockTotal,
      int stockReserved,
      ListingCondition condition,
      Category category,
      String region,
      String commune,
      UserId seller,
      ListingStatus status,
      List<String> images,
      Long version) {
    return new Listing(
        title,
        description,
        price,
        stockTotal,
        stockReserved,
        condition,
        category,
        region,
        commune,
        seller,
        id,
        version,
        status,
        images);
  }

  public void addImage(String image) {
    if (image == null || image.isBlank()) {
      throw invalid("La referencia de imagen es obligatoria.");
    }
    if (images.size() == MAX_IMAGES) {
      throw invalid("Una publicación admite como máximo 8 imágenes (RN-12).");
    }
    images.add(image.strip());
  }

  public void publish() {
    requireStatus(ListingStatus.BORRADOR, "publicar");
    if (images.isEmpty()) {
      throw new BusinessRuleViolationException(
          "LISTING_IMAGE_REQUIRED", "Para publicar debes agregar al menos una imagen (RN-12).");
    }
    status = ListingStatus.ACTIVA;
    if (availableStock() == 0) {
      status = ListingStatus.AGOTADA;
    }
  }

  public void pause() {
    requireStatus(ListingStatus.ACTIVA, "pausar");
    status = ListingStatus.PAUSADA;
  }

  public void reactivate() {
    requireStatus(ListingStatus.PAUSADA, "reactivar");
    status = ListingStatus.ACTIVA;
  }

  /** Reserva unidades para una compra; RN-05 y RN-10. */
  public void reserveStock(int quantity) {
    requireStatus(ListingStatus.ACTIVA, "reservar stock");
    if (quantity <= 0 || quantity > availableStock()) {
      throw new DomainValidationException("INVALID_STOCK_RESERVATION", "Stock no disponible.");
    }
    stockReserved += quantity;
    if (availableStock() == 0) {
      status = ListingStatus.AGOTADA;
    }
  }

  /** Repone unidades y vuelve a ACTIVA una publicación agotada (RN-10). */
  public void restock(int quantity) {
    if (quantity <= 0) {
      throw new DomainValidationException("INVALID_STOCK", "La reposición debe ser positiva.");
    }
    requireStatus(ListingStatus.AGOTADA, "reponer");
    try {
      stockTotal = Math.addExact(stockTotal, quantity);
    } catch (ArithmeticException exception) {
      throw new DomainValidationException("INVALID_STOCK", "El stock excede el máximo admitido.");
    }
    status = ListingStatus.ACTIVA;
  }

  public void block() {
    requireStatus(ListingStatus.ACTIVA, "bloquear");
    status = ListingStatus.BLOQUEADA;
  }

  public void delete() {
    if (status != ListingStatus.BORRADOR
        && status != ListingStatus.ACTIVA
        && status != ListingStatus.PAUSADA
        && status != ListingStatus.AGOTADA) {
      throw invalidTransition("eliminar");
    }
    status = ListingStatus.ELIMINADA;
  }

  public UUID id() {
    return id;
  }

  /** Versión usada para el bloqueo optimista requerido por RN-05; {@code null} antes del alta. */
  public Long version() {
    return version;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public Money price() {
    return price;
  }

  public int stockTotal() {
    return stockTotal;
  }

  public int stockReserved() {
    return stockReserved;
  }

  public int availableStock() {
    return stockTotal - stockReserved;
  }

  public ListingCondition condition() {
    return condition;
  }

  public Category category() {
    return category;
  }

  public String region() {
    return region;
  }

  public String commune() {
    return commune;
  }

  public UserId seller() {
    return seller;
  }

  public ListingStatus status() {
    return status;
  }

  public List<String> images() {
    return List.copyOf(images);
  }

  private void requireStatus(ListingStatus expected, String action) {
    if (status != expected) {
      throw invalidTransition(action);
    }
  }

  private BusinessRuleViolationException invalidTransition(String action) {
    return new BusinessRuleViolationException(
        "INVALID_LISTING_TRANSITION", "No se puede " + action + " una publicación " + status + ".");
  }

  private static String requiredLocation(String value, String field) {
    if (value == null || value.isBlank() || value.strip().length() > 60) {
      throw invalid("La " + field + " debe tener entre 1 y 60 caracteres.");
    }
    return value.strip();
  }

  private static DomainValidationException invalid(String message) {
    return new DomainValidationException("INVALID_LISTING", message);
  }
}
