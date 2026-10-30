package com.shopflow.catalog.application.service;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultation d'un produit par son vendeur, avec un faux dépôt en mémoire.
 */
class FindSellerProductServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final FindSellerProductService findSellerProductService = new FindSellerProductService(productRepository);

    @Test
    @DisplayName("SHOULD return the product WHEN it belongs to the seller")
    void shouldReturnProductWhenItBelongsToSeller() {
        // GIVEN
        Product airMax = savedProductOf(KARIM);

        // WHEN
        Optional<Product> found = findSellerProductService.findSellerProduct(KARIM, airMax.getId());

        // THEN
        assertThat(found).contains(airMax);
    }

    @Test
    @DisplayName("SHOULD return nothing WHEN the product belongs to another seller")
    void shouldReturnNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = savedProductOf(KARIM);

        // WHEN
        Optional<Product> found = findSellerProductService.findSellerProduct(LEA, airMax.getId());

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SHOULD return nothing WHEN the product does not exist")
    void shouldReturnNothingWhenProductDoesNotExist() {
        // WHEN
        Optional<Product> found = findSellerProductService.findSellerProduct(KARIM, ProductId.newId());

        // THEN
        assertThat(found).isEmpty();
    }

    private Product savedProductOf(final SellerId sellerId) {
        Product product = Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
        productRepository.save(product);
        return product;
    }
}
