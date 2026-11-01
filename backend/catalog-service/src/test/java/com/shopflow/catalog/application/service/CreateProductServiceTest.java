package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le cas d'usage avec de faux adaptateurs en mémoire : ni MongoDB, ni Kafka, ni Spring.
 */
class CreateProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final SellerId KARIM = new SellerId("seller-karim");

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final RecordingProductEventPublisher productEventPublisher = new RecordingProductEventPublisher();
    private final CreateProductService createProductService =
            new CreateProductService(productRepository, productEventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("SHOULD create a draft at the clock's time and save it WHEN the command is valid")
    void shouldCreateAndSaveDraftWhenCommandIsValid() {
        // GIVEN
        CreateProductCommand command = airMaxCommand("Air Max 90 Infrared");

        // WHEN
        Product product = createProductService.createProduct(command);

        // THEN
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCreatedAt()).isEqualTo(NOW);
        assertThat(productRepository.findById(product.getId())).contains(product);
    }

    @Test
    @DisplayName("SHOULD announce the product WHEN it has been created")
    void shouldAnnounceProductWhenCreated() {
        // GIVEN
        CreateProductCommand command = airMaxCommand("Air Max 90 Infrared");

        // WHEN
        Product product = createProductService.createProduct(command);

        // THEN
        assertThat(productEventPublisher.announced).containsExactly(product);
    }

    private static CreateProductCommand airMaxCommand(final String name) {
        return CreateProductCommand.builder().sellerId(KARIM).name(name).brand("Nike").category("sneakers").build();
    }

    /** Faux publieur : retient les produits annoncés. */
    private static final class RecordingProductEventPublisher implements ProductEventPublisher {

        private final List<Product> announced = new ArrayList<>();

        @Override
        public void publishProductCreated(final Product product) {
            announced.add(product);
        }

        @Override
        public void publishProductVariantAdded(final ProductId productId, final Variant variant) {
            throw new UnsupportedOperationException("creating a product adds no variant");
        }
    }
}
