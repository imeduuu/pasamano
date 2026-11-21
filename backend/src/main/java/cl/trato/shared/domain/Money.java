package cl.trato.shared.domain;

import cl.trato.shared.domain.exception.DomainValidationException;

/**
 * Monto en pesos chilenos (CLP). RN-01: los montos son enteros, sin decimales; un precio va de $100
 * a $99.999.999.
 *
 * @param amount monto en pesos, nunca negativo
 */
public record Money(long amount) implements Comparable<Money> {

  public static final long MIN_PRICE = 100L;
  public static final long MAX_PRICE = 99_999_999L;

  private static final Money ZERO = new Money(0);

  public Money {
    if (amount < 0) {
      throw new DomainValidationException("MONEY_NEGATIVE", "El monto no puede ser negativo.");
    }
  }

  /** Crea un monto general (totales, subtotales, saldos). */
  public static Money of(long amount) {
    return new Money(amount);
  }

  public static Money zero() {
    return ZERO;
  }

  /** Crea un precio de publicación validando RN-01. */
  public static Money price(long amount) {
    if (amount < MIN_PRICE || amount > MAX_PRICE) {
      throw new DomainValidationException(
          "PRICE_OUT_OF_RANGE", "El precio debe estar entre $100 y $99.999.999.");
    }
    return new Money(amount);
  }

  public boolean isValidPrice() {
    return amount >= MIN_PRICE && amount <= MAX_PRICE;
  }

  public boolean isZero() {
    return amount == 0;
  }

  public Money plus(Money other) {
    try {
      return new Money(Math.addExact(amount, other.amount));
    } catch (ArithmeticException e) {
      throw overflow();
    }
  }

  public Money minus(Money other) {
    return new Money(amount - other.amount);
  }

  public Money times(int quantity) {
    if (quantity < 0) {
      throw new DomainValidationException(
          "QUANTITY_NEGATIVE", "La cantidad no puede ser negativa.");
    }
    try {
      return new Money(Math.multiplyExact(amount, quantity));
    } catch (ArithmeticException e) {
      throw overflow();
    }
  }

  public boolean isGreaterThan(Money other) {
    return compareTo(other) > 0;
  }

  @Override
  public int compareTo(Money other) {
    return Long.compare(amount, other.amount);
  }

  /** Formato es-CL: signo peso y punto como separador de miles (por ejemplo, $25.990). */
  @Override
  public String toString() {
    String digits = Long.toString(amount);
    StringBuilder grouped = new StringBuilder();
    int firstGroup = digits.length() % 3;
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (i - firstGroup) % 3 == 0) {
        grouped.append('.');
      }
      grouped.append(digits.charAt(i));
    }
    return "$" + grouped;
  }

  private static DomainValidationException overflow() {
    return new DomainValidationException("MONEY_OVERFLOW", "El monto excede el máximo admitido.");
  }
}
