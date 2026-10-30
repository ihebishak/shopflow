package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.InvalidProductException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Le cas d'usage avec un faux dépôt en mémoire : ni MongoDB ni Spring, quelques millisecondes.
 */
class CreateProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final SellerId KARIM = new SellerId("seller-karim");

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final CreateProductService createProductService =
            new CreateProductService(productRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("SHOULD create a draft at the clock's time and save it WHEN the command is valid")
    void shouldCreateAndSaveDraftWhenCommandIsValid() {
        // GIVEN
        CreateProductCommand command = CreateProductCommand.builder()
                .sellerId(KARIM).name("Air Max 90 Infrared").brand("Nike").category("sneakers").build();

        // WHEN
        Product product = createProductService.createProduct(command);

        // THEN
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCreatedAt()).isEqualTo(NOW);
        assertThat(productRepository.findById(product.getId())).contains(product);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN a rule is broken")
    void shouldSaveNothingWhenRuleIsBroken() {
        // GIVEN
        CreateProductCommand command = CreateProductCommand.builder()
                .sellerId(KARIM).name(" ").brand("Nike").category("sneakers").build();

        // WHEN
        Throwable error = catchThrowable(() -> createProductService.createProduct(command));

        // THEN
        assertThat(error).isInstanceOf(InvalidProductException.class);
        assertThat(productRepository.isEmpty()).isTrue();
    }
}
