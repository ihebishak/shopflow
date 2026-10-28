package com.shopflow.kernel.money;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void adds_and_subtracts_amounts_of_the_same_currency() {
        assertThat(Money.euros(16_000).plus(Money.euros(2_999))).isEqualTo(Money.euros(18_999));
        assertThat(Money.euros(16_000).minus(Money.euros(1_600))).isEqualTo(Money.euros(14_400));
    }

    @Test
    void multiplies_by_a_quantity() {
        assertThat(Money.euros(12_000).times(3)).isEqualTo(Money.euros(36_000));
    }

    @Test
    void commission_is_rounded_down_to_the_cent() {
        // 10 % de 159,99 € = 15,999 € -> 15,99 € : le vendeur n'est jamais lésé
        assertThat(Money.euros(15_999).percentageFloor(1_000)).isEqualTo(Money.euros(1_599));
        assertThat(Money.euros(16_000).percentageFloor(1_000)).isEqualTo(Money.euros(1_600));
    }

    @Test
    void refuses_to_mix_currencies() {
        assertThatThrownBy(() -> Money.euros(100).plus(Money.of(100, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void refuses_overflow_instead_of_silently_wrapping() {
        assertThatThrownBy(() -> Money.euros(Long.MAX_VALUE).plus(Money.euros(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void formats_with_currency_fraction_digits() {
        assertThat(Money.euros(15_999)).hasToString("159.99 EUR");
        assertThat(Money.euros(-5)).hasToString("-0.05 EUR");
        assertThat(Money.of(1_500, "JPY")).hasToString("1500 JPY");
    }

    @Test
    void compares_amounts() {
        assertThat(Money.euros(2_000)).isGreaterThan(Money.euros(1_999));
    }
}
