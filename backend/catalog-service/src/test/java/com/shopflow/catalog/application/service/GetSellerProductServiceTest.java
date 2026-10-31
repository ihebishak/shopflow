package com.shopflow.catalog.application.service;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.kernel.error.ErrorKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Consultation d'un produit par son vendeur, avec un faux dépôt en mémoire.
 */
class GetSellerProductServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final GetSellerProductService getSellerProductService = new GetSellerProductService(productRepository);

    @Test
    @DisplayName("SHOULD return the product WHEN it belongs to the seller")
    void shouldReturnProductWhenItBelongsToSeller() {
        // GIVEN
        Product airMax = savedProductOf(KARIM);

        // WHEN
        Product found = getSellerProductService.getSellerProduct(KARIM, airMax.getId());

        // THEN
        assertThat(found).isSameAs(airMax);
    }

    @Test
    @DisplayName("SHOULD throw ProductNotFoundException WHEN the product belongs to another seller")
    void shouldThrowNotFoundWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = savedProductOf(KARIM);

        // WHEN
        Throwable error = catchThrowable(() -> getSellerProductService.getSellerProduct(LEA, airMax.getId()));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product " + airMax.getId() + " not found");
    }

    @Test
    @DisplayName("SHOULD throw ProductNotFoundException WHEN the product does not exist")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        // GIVEN
        ProductId unknown = ProductId.newId();

        // WHEN
        Throwable error = catchThrowable(() -> getSellerProductService.getSellerProduct(KARIM, unknown));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        ProductNotFoundException notFound = (ProductNotFoundException) error;
        assertThat(notFound.getCode()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(notFound.getKind()).isEqualTo(ErrorKind.NOT_FOUND);
    }

    private Product savedProductOf(final SellerId sellerId) {
        Product product = Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
        productRepository.save(product);
        return product;
    }
}
