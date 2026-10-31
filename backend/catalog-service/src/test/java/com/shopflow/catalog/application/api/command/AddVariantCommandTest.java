package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.kernel.money.Money;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règles de format d'une variante, portées par les annotations de la commande.
 */
class AddVariantCommandTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String PRICE_ERROR = "price must be a positive amount in euros";

    @Test
    @DisplayName("SHOULD accept the command WHEN it is valid")
    void shouldAcceptCommandWhenValid() {
        // WHEN
        List<String> errors = errorsOf(variant().build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD accept the command WHEN each field is at its maximum length")
    void shouldAcceptCommandWhenEachFieldIsAtItsMaximumLength() {
        // GIVEN
        AddVariantCommand command = variant().size("a".repeat(20)).color("b".repeat(40)).build();

        // WHEN
        List<String> errors = errorsOf(command);

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD accept the command WHEN the price is one cent")
    void shouldAcceptCommandWhenPriceIsOneCent() {
        // WHEN
        List<String> errors = errorsOf(variant().price(Money.euros(1)).build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the seller is missing")
    void shouldRefuseCommandWhenSellerIsMissing() {
        // WHEN
        List<String> errors = errorsOf(variant().sellerId(null).build());

        // THEN
        assertThat(errors).containsExactly("sellerId is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the product is missing")
    void shouldRefuseCommandWhenProductIsMissing() {
        // WHEN
        List<String> errors = errorsOf(variant().productId(null).build());

        // THEN
        assertThat(errors).containsExactly("productId is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD refuse the command WHEN the size is missing or blank")
    void shouldRefuseCommandWhenSizeIsMissingOrBlank(final String size) {
        // WHEN
        List<String> errors = errorsOf(variant().size(size).build());

        // THEN
        assertThat(errors).containsExactly("size is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD refuse the command WHEN the color is missing or blank")
    void shouldRefuseCommandWhenColorIsMissingOrBlank(final String color) {
        // WHEN
        List<String> errors = errorsOf(variant().color(color).build());

        // THEN
        assertThat(errors).containsExactly("color is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the size is too long")
    void shouldRefuseCommandWhenSizeIsTooLong() {
        // WHEN
        List<String> errors = errorsOf(variant().size("a".repeat(21)).build());

        // THEN
        assertThat(errors).containsExactly("size must be at most 20 characters");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the color is too long")
    void shouldRefuseCommandWhenColorIsTooLong() {
        // WHEN
        List<String> errors = errorsOf(variant().color("a".repeat(41)).build());

        // THEN
        assertThat(errors).containsExactly("color must be at most 40 characters");
    }

    @Test
    @DisplayName("SHOULD report a single error WHEN the price is missing")
    void shouldReportSingleErrorWhenPriceIsMissing() {
        // WHEN
        List<String> errors = errorsOf(variant().price(null).build());

        // THEN
        assertThat(errors).containsExactly("price is required");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("SHOULD refuse the command WHEN the price is not positive")
    void shouldRefuseCommandWhenPriceIsNotPositive(final long cents) {
        // WHEN
        List<String> errors = errorsOf(variant().price(Money.euros(cents)).build());

        // THEN
        assertThat(errors).containsExactly(PRICE_ERROR);
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the price is not in euros")
    void shouldRefuseCommandWhenPriceIsNotInEuros() {
        // WHEN
        List<String> errors = errorsOf(variant().price(Money.of(16_000, "USD")).build());

        // THEN
        assertThat(errors).containsExactly(PRICE_ERROR);
    }

    private static AddVariantCommand.AddVariantCommandBuilder variant() {
        return AddVariantCommand.builder().sellerId(new SellerId("seller-karim")).productId(ProductId.newId())
                .size("42").color("Rouge").price(Money.euros(16_000));
    }

    private static List<String> errorsOf(final AddVariantCommand command) {
        return VALIDATOR.validate(command).stream().map(ConstraintViolation::getMessage).toList();
    }
}
