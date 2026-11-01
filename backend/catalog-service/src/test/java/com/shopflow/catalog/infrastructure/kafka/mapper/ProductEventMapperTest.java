package com.shopflow.catalog.infrastructure.kafka.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductPriceChanged;
import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.contracts.product.ProductUpdated;
import com.shopflow.contracts.product.ProductVariantAdded;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

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

    @Test
    @DisplayName("SHOULD copy the product id and every variant field WHEN mapping to ProductVariantAdded")
    void shouldCopyEveryFieldWhenMappingVariantToProductVariantAdded() {
        // GIVEN
        ProductId productId = ProductId.newId();
        Variant variant = Variant.restore(Sku.newSku(), "42", "Rouge", Money.euros(16_000));

        // WHEN
        ProductVariantAdded event = productEventMapper.toProductVariantAdded(productId, variant);

        // THEN
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getVariant().getSku()).isEqualTo(variant.getSku().toString());
        assertThat(event.getVariant().getSize()).isEqualTo("42");
        assertThat(event.getVariant().getColor()).isEqualTo("Rouge");
        assertThat(event.getVariant().getPrice().getAmount()).isEqualTo(16_000);
        assertThat(event.getVariant().getPrice().getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("SHOULD copy the product id and the changed fields WHEN mapping to ProductUpdated")
    void shouldCopyEveryFieldWhenMappingToProductUpdated() {
        // GIVEN
        ProductId productId = ProductId.newId();

        // WHEN
        ProductUpdated event = productEventMapper.toProductUpdated(productId, List.of("name", "description"));

        // THEN
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getChangedFields()).containsExactly("name", "description");
    }

    @Test
    @DisplayName("SHOULD copy the product, the sku and both prices WHEN mapping to ProductPriceChanged")
    void shouldCopyEveryFieldWhenMappingToProductPriceChanged() {
        // GIVEN
        ProductId productId = ProductId.newId();
        Sku sku = Sku.newSku();

        // WHEN
        ProductPriceChanged event = productEventMapper.toProductPriceChanged(productId, sku, Money.euros(16_000),
                Money.euros(12_900));

        // THEN
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getSku()).isEqualTo(sku.toString());
        assertThat(event.getOldPrice().getAmount()).isEqualTo(16_000);
        assertThat(event.getOldPrice().getCurrency()).isEqualTo("EUR");
        assertThat(event.getNewPrice().getAmount()).isEqualTo(12_900);
        assertThat(event.getNewPrice().getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("SHOULD copy the product id and the seller WHEN mapping to ProductPublished")
    void shouldCopyEveryFieldWhenMappingToProductPublished() {
        // GIVEN
        Product airMax = Product.draft(new SellerId("seller-karim"), "Air Max 90 Infrared", "Nike", "sneakers", null,
                Instant.now());

        // WHEN
        ProductPublished event = productEventMapper.toProductPublished(airMax);

        // THEN
        assertThat(event.getProductId()).isEqualTo(airMax.getId().toString());
        assertThat(event.getSellerId()).isEqualTo("seller-karim");
    }
}
