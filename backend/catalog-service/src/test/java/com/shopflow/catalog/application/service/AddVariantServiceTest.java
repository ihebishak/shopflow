package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.DuplicateVariantException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Déroulé du cas d'usage, sans Spring ni base de données.
 */
class AddVariantServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");
    private static final Money PRICE = Money.euros(16_000);

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductEventPublisher productEventPublisher = mock(ProductEventPublisher.class);
    private final AddVariantService addVariantService = new AddVariantService(
            new GetSellerProductService(productRepository), productRepository, productEventPublisher);

    @Test
    @DisplayName("SHOULD add the variant and save the product WHEN the product belongs to the seller")
    void shouldAddVariantAndSaveProductWhenProductBelongsToSeller() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Variant variant = addVariantService.addVariant(commandOf(KARIM, airMax.getId(), "42"));

        // THEN
        assertThat(variant.getSize()).isEqualTo("42");
        assertThat(variant.getColor()).isEqualTo("Rouge");
        assertThat(variant.getPrice()).isEqualTo(PRICE);
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(saved.capture());
        saveThenPublish.verify(productEventPublisher).publishProductVariantAdded(airMax.getId(), variant);
        assertThat(saved.getValue().getId()).isEqualTo(airMax.getId());
        assertThat(saved.getValue().getVariants()).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product belongs to another seller")
    void shouldSaveNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Throwable error = catchThrowable(() -> addVariantService.addVariant(commandOf(LEA, airMax.getId(), "42")));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        assertThat(airMax.getVariants()).isEmpty();
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product does not exist")
    void shouldSaveNothingWhenProductDoesNotExist() {
        // GIVEN
        ProductId unknown = ProductId.newId();
        when(productRepository.findById(unknown)).thenReturn(Optional.empty());

        // WHEN
        Throwable error = catchThrowable(() -> addVariantService.addVariant(commandOf(KARIM, unknown, "42")));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the variant already exists")
    void shouldSaveNothingWhenVariantAlreadyExists() {
        // GIVEN
        Product airMax = existing(draftOf(KARIM).addVariant(Variant.create("42", "Rouge", PRICE)));

        // WHEN
        Throwable error = catchThrowable(() -> addVariantService.addVariant(commandOf(KARIM, airMax.getId(), "42")));

        // THEN
        assertThat(error).isInstanceOf(DuplicateVariantException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD publish nothing WHEN the product changed before it could be saved")
    void shouldPublishNothingWhenProductChangedBeforeBeingSaved() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);
        doThrow(new ProductModifiedConcurrentlyException(airMax.getId())).when(productRepository).save(any());

        // WHEN
        Throwable error = catchThrowable(() -> addVariantService.addVariant(commandOf(KARIM, airMax.getId(), "42")));

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        verifyNoInteractions(productEventPublisher);
    }

    private Product existingProductOf(final SellerId sellerId) {
        return existing(draftOf(sellerId));
    }

    private Product existing(final Product product) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private static Product draftOf(final SellerId sellerId) {
        return Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
    }

    private static AddVariantCommand commandOf(final SellerId sellerId, final ProductId productId, final String size) {
        return AddVariantCommand.builder().sellerId(sellerId).productId(productId).size(size).color("Rouge")
                .price(PRICE).build();
    }
}
