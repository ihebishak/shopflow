package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.kernel.money.Money;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règles de format d'un changement de prix, portées par les annotations de la commande.
 */
class ChangeVariantPriceCommandTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String PRICE_ERROR = "price must be a positive amount in euros";

    @Test
    @DisplayName("SHOULD accept the command WHEN it is valid")
    void shouldAcceptCommandWhenValid() {
        // WHEN
        List<String> errors = errorsOf(priceChange().build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD accept the command WHEN the price is one cent")
    void shouldAcceptCommandWhenPriceIsOneCent() {
        // WHEN
        List<String> errors = errorsOf(priceChange().price(Money.euros(1)).build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the seller is missing")
    void shouldRefuseCommandWhenSellerIsMissing() {
        // WHEN
        List<String> errors = errorsOf(priceChange().sellerId(null).build());

        // THEN
        assertThat(errors).containsExactly("sellerId is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the product is missing")
    void shouldRefuseCommandWhenProductIsMissing() {
        // WHEN
        List<String> errors = errorsOf(priceChange().productId(null).build());

        // THEN
        assertThat(errors).containsExactly("productId is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the sku is missing")
    void shouldRefuseCommandWhenSkuIsMissing() {
        // WHEN
        List<String> errors = errorsOf(priceChange().sku(null).build());

        // THEN
        assertThat(errors).containsExactly("sku is required");
    }

    @Test
    @DisplayName("SHOULD report a single error WHEN the price is missing")
    void shouldReportSingleErrorWhenPriceIsMissing() {
        // WHEN
        List<String> errors = errorsOf(priceChange().price(null).build());

        // THEN
        assertThat(errors).containsExactly("price is required");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("SHOULD refuse the command WHEN the price is not positive")
    void shouldRefuseCommandWhenPriceIsNotPositive(final long cents) {
        // WHEN
        List<String> errors = errorsOf(priceChange().price(Money.euros(cents)).build());

        // THEN
        assertThat(errors).containsExactly(PRICE_ERROR);
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the price is not in euros")
    void shouldRefuseCommandWhenPriceIsNotInEuros() {
        // WHEN
        List<String> errors = errorsOf(priceChange().price(Money.of(16_000, "USD")).build());

        // THEN
        assertThat(errors).containsExactly(PRICE_ERROR);
    }

    private static ChangeVariantPriceCommand.ChangeVariantPriceCommandBuilder priceChange() {
        return ChangeVariantPriceCommand.builder().sellerId(new SellerId("seller-karim")).productId(ProductId.newId())
                .sku(Sku.newSku()).price(Money.euros(12_900));
    }

    private static List<String> errorsOf(final ChangeVariantPriceCommand command) {
        return VALIDATOR.validate(command).stream().map(ConstraintViolation::getMessage).toList();
    }
}
