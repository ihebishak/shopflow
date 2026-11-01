package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.PublishProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.ProductWithoutVariantException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class PublishProductServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductEventPublisher productEventPublisher = mock(ProductEventPublisher.class);
    private final PublishProductService publishProductService = new PublishProductService(
            new GetSellerProductService(productRepository), productRepository, productEventPublisher);

    @Test
    @DisplayName("SHOULD save the product then announce it WHEN a draft is published")
    void shouldSaveProductThenAnnounceItWhenDraftIsPublished() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM));

        // WHEN
        Product published = publishProductService.publishProduct(commandOf(KARIM, airMax.getId()));

        // THEN
        assertThat(published.getId()).isEqualTo(airMax.getId());
        assertThat(published.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(published);
        saveThenPublish.verify(productEventPublisher).publishProductPublished(published);
    }

    @Test
    @DisplayName("SHOULD save the product then announce it WHEN an unpublished product is published again")
    void shouldSaveProductThenAnnounceItWhenUnpublishedProductIsPublishedAgain() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish().unpublish());

        // WHEN
        Product published = publishProductService.publishProduct(commandOf(KARIM, airMax.getId()));

        // THEN
        assertThat(published.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(published);
        saveThenPublish.verify(productEventPublisher).publishProductPublished(published);
    }

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN the product is already published")
    void shouldSaveAndAnnounceNothingWhenProductIsAlreadyPublished() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish());

        // WHEN
        Product unchanged = publishProductService.publishProduct(commandOf(KARIM, airMax.getId()));

        // THEN
        assertThat(unchanged).isSameAs(airMax);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product has no variant")
    void shouldSaveNothingWhenProductHasNoVariant() {
        // GIVEN
        Product airMax = stored(draftOf(KARIM));

        // WHEN
        Throwable error = catchThrowable(() -> publishProductService.publishProduct(commandOf(KARIM, airMax.getId())));

        // THEN
        assertThat(error).isInstanceOf(ProductWithoutVariantException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product belongs to another seller")
    void shouldSaveNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM));

        // WHEN
        Throwable error = catchThrowable(() -> publishProductService.publishProduct(commandOf(LEA, airMax.getId())));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
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
        Throwable error = catchThrowable(() -> publishProductService.publishProduct(commandOf(KARIM, unknown)));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD announce nothing WHEN the product changed before it could be saved")
    void shouldAnnounceNothingWhenProductChangedBeforeBeingSaved() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM));
        doThrow(new ProductModifiedConcurrentlyException(airMax.getId())).when(productRepository).save(any());

        // WHEN
        Throwable error = catchThrowable(() -> publishProductService.publishProduct(commandOf(KARIM, airMax.getId())));

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        verifyNoInteractions(productEventPublisher);
    }

    private Product stored(final Product product) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private static Product draftWithVariant(final SellerId sellerId) {
        return draftOf(sellerId).addVariant(Variant.create("42", "Rouge", Money.euros(16_000)));
    }

    private static Product draftOf(final SellerId sellerId) {
        return Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
    }

    private static PublishProductCommand commandOf(final SellerId sellerId, final ProductId productId) {
        return PublishProductCommand.builder().sellerId(sellerId).productId(productId).build();
    }
}
