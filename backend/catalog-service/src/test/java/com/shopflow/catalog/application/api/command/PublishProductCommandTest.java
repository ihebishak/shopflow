package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règles de format d'une publication, portées par les annotations de la commande.
 */
class PublishProductCommandTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("SHOULD accept the command WHEN it is valid")
    void shouldAcceptCommandWhenValid() {
        // WHEN
        List<String> errors = errorsOf(publication().build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the seller is missing")
    void shouldRefuseCommandWhenSellerIsMissing() {
        // WHEN
        List<String> errors = errorsOf(publication().sellerId(null).build());

        // THEN
        assertThat(errors).containsExactly("sellerId is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the product is missing")
    void shouldRefuseCommandWhenProductIsMissing() {
        // WHEN
        List<String> errors = errorsOf(publication().productId(null).build());

        // THEN
        assertThat(errors).containsExactly("productId is required");
    }

    private static PublishProductCommand.PublishProductCommandBuilder publication() {
        return PublishProductCommand.builder().sellerId(new SellerId("seller-karim")).productId(ProductId.newId());
    }

    private static List<String> errorsOf(final PublishProductCommand command) {
        return VALIDATOR.validate(command).stream().map(ConstraintViolation::getMessage).toList();
    }
}
