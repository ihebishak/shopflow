package com.shopflow.catalog.application.api.query;

import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
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
 * Règles de format d'une liste de produits, portées par les annotations de la demande.
 */
class ListSellerProductsQueryTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String SIZE_ERROR = "size must be between 1 and 100";

    @Test
    @DisplayName("SHOULD accept the query WHEN it has no status")
    void shouldAcceptQueryWhenItHasNoStatus() {
        // WHEN
        List<String> errors = errorsOf(firstPage().status(null).build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD accept the query WHEN it has a status")
    void shouldAcceptQueryWhenItHasStatus() {
        // WHEN
        List<String> errors = errorsOf(firstPage().status(ProductStatus.PUBLISHED).build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the query WHEN the seller is missing")
    void shouldRefuseQueryWhenSellerIsMissing() {
        // WHEN
        List<String> errors = errorsOf(firstPage().sellerId(null).build());

        // THEN
        assertThat(errors).containsExactly("sellerId is required");
    }

    @Test
    @DisplayName("SHOULD refuse the query WHEN the page is negative")
    void shouldRefuseQueryWhenPageIsNegative() {
        // WHEN
        List<String> errors = errorsOf(firstPage().page(-1).build());

        // THEN
        assertThat(errors).containsExactly("page must not be negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    @DisplayName("SHOULD accept the query WHEN the size is at a limit")
    void shouldAcceptQueryWhenSizeIsAtLimit(final int size) {
        // WHEN
        List<String> errors = errorsOf(firstPage().size(size).build());

        // THEN
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    @DisplayName("SHOULD refuse the query WHEN the size is out of the limits")
    void shouldRefuseQueryWhenSizeIsOutOfLimits(final int size) {
        // WHEN
        List<String> errors = errorsOf(firstPage().size(size).build());

        // THEN
        assertThat(errors).containsExactly(SIZE_ERROR);
    }

    private static ListSellerProductsQuery.ListSellerProductsQueryBuilder firstPage() {
        return ListSellerProductsQuery.builder().sellerId(new SellerId("seller-karim")).page(0).size(20);
    }

    private static List<String> errorsOf(final ListSellerProductsQuery query) {
        return VALIDATOR.validate(query).stream().map(ConstraintViolation::getMessage).toList();
    }
}
