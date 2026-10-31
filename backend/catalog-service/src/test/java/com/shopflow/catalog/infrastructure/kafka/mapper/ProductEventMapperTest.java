package com.shopflow.catalog.infrastructure.kafka.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.contracts.product.ProductCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le code généré par MapStruct, testé sans Spring ni Kafka.
 */
class ProductEventMapperTest {

    private final ProductEventMapper productEventMapper = new ProductEventMapperImpl();

    @Test
    @DisplayName("SHOULD copy every field with no variant WHEN mapping a new product to ProductCreated")
    void shouldCopyEveryFieldWhenMappingProductToProductCreated() {
        // GIVEN
        Product airMax = Product.draft(new SellerId("seller-karim"), "Air Max 90 Infrared", "Nike", "sneakers",
                "Réédition 1990", Instant.now());

        // WHEN
        ProductCreated event = productEventMapper.toProductCreated(airMax);

        // THEN
        assertThat(event.getProductId()).isEqualTo(airMax.getId().toString());
        assertThat(event.getSellerId()).isEqualTo("seller-karim");
        assertThat(event.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(event.getBrand()).isEqualTo("Nike");
        assertThat(event.getCategory()).isEqualTo("sneakers");
        assertThat(event.getVariants()).isEmpty();
    }
}
