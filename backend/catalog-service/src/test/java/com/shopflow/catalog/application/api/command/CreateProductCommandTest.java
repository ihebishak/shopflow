package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.SellerId;
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
 * Règles de format d'un produit, portées par les annotations de la commande.
 */
class CreateProductCommandTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("SHOULD accept the command WHEN the description is left out")
    void shouldAcceptCommandWhenDescriptionIsLeftOut() {
        // WHEN
        List<String> errors = errorsOf(airMax().build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD accept the command WHEN each field is at its maximum length")
    void shouldAcceptCommandWhenEachFieldIsAtItsMaximumLength() {
        // GIVEN
        CreateProductCommand command = airMax().name("a".repeat(120)).brand("b".repeat(60)).category("c".repeat(60))
                .description("d".repeat(2_000)).build();

        // WHEN
        List<String> errors = errorsOf(command);

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the seller is missing")
    void shouldRefuseCommandWhenSellerIsMissing() {
        // WHEN
        List<String> errors = errorsOf(airMax().sellerId(null).build());

        // THEN
        assertThat(errors).containsExactly("sellerId is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD refuse the command WHEN the name is missing or blank")
    void shouldRefuseCommandWhenNameIsMissingOrBlank(final String name) {
        // WHEN
        List<String> errors = errorsOf(airMax().name(name).build());

        // THEN
        assertThat(errors).containsExactly("name is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the name is longer than 120 characters")
    void shouldRefuseCommandWhenNameIsLongerThan120Characters() {
        // WHEN
        List<String> errors = errorsOf(airMax().name("a".repeat(121)).build());

        // THEN
        assertThat(errors).containsExactly("name must be at most 120 characters");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the brand is blank")
    void shouldRefuseCommandWhenBrandIsBlank() {
        // WHEN
        List<String> errors = errorsOf(airMax().brand(" ").build());

        // THEN
        assertThat(errors).containsExactly("brand is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the brand is longer than 60 characters")
    void shouldRefuseCommandWhenBrandIsLongerThan60Characters() {
        // WHEN
        List<String> errors = errorsOf(airMax().brand("b".repeat(61)).build());

        // THEN
        assertThat(errors).containsExactly("brand must be at most 60 characters");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the category is missing")
    void shouldRefuseCommandWhenCategoryIsMissing() {
        // WHEN
        List<String> errors = errorsOf(airMax().category(null).build());

        // THEN
        assertThat(errors).containsExactly("category is required");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the category is longer than 60 characters")
    void shouldRefuseCommandWhenCategoryIsLongerThan60Characters() {
        // WHEN
        List<String> errors = errorsOf(airMax().category("c".repeat(61)).build());

        // THEN
        assertThat(errors).containsExactly("category must be at most 60 characters");
    }

    @Test
    @DisplayName("SHOULD refuse the command WHEN the description is longer than 2000 characters")
    void shouldRefuseCommandWhenDescriptionIsLongerThan2000Characters() {
        // WHEN
        List<String> errors = errorsOf(airMax().description("d".repeat(2_001)).build());

        // THEN
        assertThat(errors).containsExactly("description must be at most 2000 characters");
    }

    @Test
    @DisplayName("SHOULD report every error WHEN several fields are invalid")
    void shouldReportEveryErrorWhenSeveralFieldsAreInvalid() {
        // WHEN
        List<String> errors = errorsOf(airMax().name(" ").brand(" ").build());

        // THEN
        assertThat(errors).containsExactlyInAnyOrder("name is required", "brand is required");
    }

    private static CreateProductCommand.CreateProductCommandBuilder airMax() {
        return CreateProductCommand.builder().sellerId(new SellerId("seller-karim")).name("Air Max 90 Infrared")
                .brand("Nike").category("sneakers");
    }

    private static List<String> errorsOf(final CreateProductCommand command) {
        return VALIDATOR.validate(command).stream().map(ConstraintViolation::getMessage).toList();
    }
}
