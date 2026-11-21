package cl.trato.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Money (CLP, RN-01)")
class MoneyTest {

  @Nested
  @DisplayName("montos generales")
  class GeneralAmounts {

    @Test
    void creates_an_amount_in_whole_pesos() {
      Money money = Money.of(25_990);

      assertThat(money.amount()).isEqualTo(25_990L);
    }

    @Test
    void zero_is_a_valid_amount() {
      assertThat(Money.zero().amount()).isZero();
      assertThat(Money.zero().isZero()).isTrue();
    }

    @Test
    void rejects_negative_amounts() {
      assertThatThrownBy(() -> Money.of(-1))
          .isInstanceOf(DomainValidationException.class)
          .hasMessageContaining("negativo")
          .extracting("code")
          .isEqualTo("MONEY_NEGATIVE");
    }

    @Test
    void two_amounts_with_same_value_are_equal() {
      assertThat(Money.of(100)).isEqualTo(Money.of(100)).hasSameHashCodeAs(Money.of(100));
    }
  }

  @Nested
  @DisplayName("precios (RN-01: mínimo $100 y máximo $99.999.999)")
  class Prices {

    @ParameterizedTest
    @ValueSource(longs = {100, 101, 25_990, 99_999_999})
    void accepts_prices_inside_the_range(long amount) {
      Money price = Money.price(amount);

      assertThat(price.amount()).isEqualTo(amount);
      assertThat(price.isValidPrice()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 99, 100_000_000, Long.MAX_VALUE})
    void rejects_prices_outside_the_range(long amount) {
      assertThatThrownBy(() -> Money.price(amount))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("PRICE_OUT_OF_RANGE");
    }

    @Test
    void a_general_amount_outside_the_price_range_is_not_a_valid_price() {
      assertThat(Money.of(50).isValidPrice()).isFalse();
      assertThat(Money.of(150_000_000).isValidPrice()).isFalse();
    }
  }

  @Nested
  @DisplayName("aritmética")
  class Arithmetic {

    @Test
    void adds_amounts() {
      assertThat(Money.of(1_000).plus(Money.of(2_500))).isEqualTo(Money.of(3_500));
    }

    @Test
    void subtracts_amounts() {
      assertThat(Money.of(3_500).minus(Money.of(1_000))).isEqualTo(Money.of(2_500));
    }

    @Test
    void subtraction_cannot_produce_a_negative_amount() {
      assertThatThrownBy(() -> Money.of(100).minus(Money.of(101)))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("MONEY_NEGATIVE");
    }

    @Test
    void multiplies_by_a_quantity() {
      assertThat(Money.of(12_990).times(3)).isEqualTo(Money.of(38_970));
      assertThat(Money.of(12_990).times(0)).isEqualTo(Money.zero());
    }

    @Test
    void rejects_negative_quantities() {
      assertThatThrownBy(() -> Money.of(100).times(-1))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("QUANTITY_NEGATIVE");
    }

    @Test
    void detects_overflow_instead_of_wrapping_around() {
      Money huge = Money.of(Long.MAX_VALUE);

      assertThatThrownBy(() -> huge.plus(Money.of(1)))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("MONEY_OVERFLOW");
      assertThatThrownBy(() -> huge.times(2))
          .isInstanceOf(DomainValidationException.class)
          .extracting("code")
          .isEqualTo("MONEY_OVERFLOW");
    }

    @Test
    void compares_amounts() {
      assertThat(Money.of(200)).isGreaterThan(Money.of(100));
      assertThat(Money.of(200).isGreaterThan(Money.of(100))).isTrue();
      assertThat(Money.of(100).isGreaterThan(Money.of(100))).isFalse();
    }
  }

  @Test
  @DisplayName("se muestra en formato es-CL con separador de miles")
  void formats_with_chilean_thousands_separator() {
    assertThat(Money.of(25_990)).hasToString("$25.990");
    assertThat(Money.of(99_999_999)).hasToString("$99.999.999");
    assertThat(Money.of(100)).hasToString("$100");
    assertThat(Money.zero()).hasToString("$0");
  }
}
