package com.shopflow.catalog.domain;

import com.shopflow.kernel.id.UuidV7;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Règles d'un produit, sans Spring ni base de données : chaque test s'exécute en quelques millisecondes.
 */
class ProductTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @Test
    void creates_a_draft_with_a_new_id_and_trimmed_fields() {
        Product product = Product.draft(KARIM, "  Air Max 90 Infrared ", " Nike", "sneakers ",
                "  La réédition du modèle de 1990  ", NOW);

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.sellerId()).isEqualTo(KARIM);
        assertThat(product.name()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.brand()).isEqualTo("Nike");
        assertThat(product.category()).isEqualTo("sneakers");
        assertThat(product.description()).contains("La réédition du modèle de 1990");
        assertThat(product.createdAt()).isEqualTo(NOW);
        assertThat(product.id().value().version()).as("UUID v7").isEqualTo(7);
        assertThat(UuidV7.timestampOf(product.id().value())).isNotNull();
    }

    @Test
    void gives_each_product_its_own_id() {
        Product first = draft("Air Max 90");
        Product second = draft("Air Max 90");

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void description_is_optional(String description) {
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", description, NOW);

        assertThat(product.description()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void refuses_a_product_without_name(String name) {
        assertThatThrownBy(() -> draft(name))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("name is required")
                .extracting("field").isEqualTo("name");
    }

    @Test
    void accepts_a_name_of_120_characters_but_not_more() {
        assertThat(draft("a".repeat(120)).name()).hasSize(120);

        assertThatThrownBy(() -> draft("a".repeat(121)))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("name must be at most 120 characters");
    }

    @Test
    void refuses_a_product_without_brand() {
        assertThatThrownBy(() -> Product.draft(KARIM, "Air Max 90", " ", "sneakers", null, NOW))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("brand is required");
    }

    @Test
    void refuses_a_brand_longer_than_60_characters() {
        assertThatThrownBy(() -> Product.draft(KARIM, "Air Max 90", "b".repeat(61), "sneakers", null, NOW))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("brand must be at most 60 characters");
    }

    @Test
    void refuses_a_product_without_category() {
        assertThatThrownBy(() -> Product.draft(KARIM, "Air Max 90", "Nike", null, null, NOW))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("category is required");
    }

    @Test
    void refuses_a_description_longer_than_2000_characters() {
        assertThatThrownBy(() -> Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "d".repeat(2_001), NOW))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("description must be at most 2000 characters");
    }

    @Test
    void a_seller_id_cannot_be_blank() {
        assertThatThrownBy(() -> new SellerId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seller id must not be blank");
    }

    private static Product draft(String name) {
        return Product.draft(KARIM, name, "Nike", "sneakers", null, NOW);
    }
}
