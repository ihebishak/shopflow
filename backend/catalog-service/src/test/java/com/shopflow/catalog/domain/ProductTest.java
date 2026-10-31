package com.shopflow.catalog.domain;

import com.shopflow.kernel.id.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Règles d'un produit, sans Spring ni base de données : chaque test s'exécute en quelques millisecondes.
 */
class ProductTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @Test
    @DisplayName("SHOULD create a draft with a new id and trimmed fields WHEN the product is valid")
    void shouldCreateDraftWhenProductIsValid() {
        // GIVEN
        String name = "  Air Max 90 Infrared ";
        String description = "  La réédition du modèle de 1990  ";

        // WHEN
        Product product = Product.draft(KARIM, name, " Nike", "sneakers ", description, NOW);

        // THEN
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getSellerId()).isEqualTo(KARIM);
        assertThat(product.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.getBrand()).isEqualTo("Nike");
        assertThat(product.getCategory()).isEqualTo("sneakers");
        assertThat(product.getDescription()).contains("La réédition du modèle de 1990");
        assertThat(product.getCreatedAt()).isEqualTo(NOW);
        assertThat(product.getId().getValue().version()).as("UUID v7").isEqualTo(7);
        assertThat(UuidV7.timestampOf(product.getId().getValue())).isNotNull();
    }

    @Test
    @DisplayName("SHOULD keep the creation date to the millisecond WHEN the clock is more precise (like MongoDB)")
    void shouldKeepCreationDateToTheMillisecondWhenClockIsMorePrecise() {
        // GIVEN
        Instant preciseNow = Instant.parse("2026-10-31T10:00:00.123456789Z");

        // WHEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", null, preciseNow);

        // THEN
        assertThat(product.getCreatedAt()).isEqualTo(Instant.parse("2026-10-31T10:00:00.123Z"));
    }

    @Test
    @DisplayName("SHOULD give each product its own id WHEN two identical products are created")
    void shouldGiveEachProductItsOwnIdWhenTwoIdenticalProductsAreCreated() {
        // GIVEN / WHEN
        Product first = draftNamed("Air Max 90");
        Product second = draftNamed("Air Max 90");

        // THEN
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD have no description WHEN the description is missing or blank")
    void shouldHaveNoDescriptionWhenDescriptionIsMissingOrBlank(final String description) {
        // WHEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", description, NOW);

        // THEN
        assertThat(product.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the seller id WHEN it is blank")
    void shouldRefuseSellerIdWhenBlank() {
        // WHEN
        Throwable error = catchThrowable(() -> new SellerId(" "));

        // THEN
        assertThat(error).isInstanceOf(InvalidSellerIdException.class).hasMessage("Seller id must not be blank");
        assertThat(((InvalidSellerIdException) error).getCode()).isEqualTo("INVALID_SELLER_ID");
    }

    private static Product draftNamed(final String name) {
        return Product.draft(KARIM, name, "Nike", "sneakers", null, NOW);
    }
}
