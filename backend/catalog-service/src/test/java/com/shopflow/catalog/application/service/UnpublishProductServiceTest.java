package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.UnpublishProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.ProductNotPublishedException;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.UnpublishReason;
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
class UnpublishProductServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductEventPublisher productEventPublisher = mock(ProductEventPublisher.class);
    private final UnpublishProductService unpublishProductService = new UnpublishProductService(
            new GetSellerProductService(productRepository), productRepository, productEventPublisher);

    @Test
    @DisplayName("SHOULD save the product then announce it with the SELLER reason WHEN a published product is unpublished")
    void shouldSaveProductThenAnnounceItWhenPublishedProductIsUnpublished() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish());

        // WHEN
        Product unpublished = unpublishProductService.unpublishProduct(commandOf(KARIM, airMax.getId()));

        // THEN
        assertThat(unpublished.getId()).isEqualTo(airMax.getId());
        assertThat(unpublished.getStatus()).isEqualTo(ProductStatus.UNPUBLISHED);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(unpublished);
        saveThenPublish.verify(productEventPublisher).publishProductUnpublished(unpublished, UnpublishReason.SELLER);
    }

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN the product is already unpublished")
    void shouldSaveAndAnnounceNothingWhenProductIsAlreadyUnpublished() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish().unpublish());

        // WHEN
        Product unchanged = unpublishProductService.unpublishProduct(commandOf(KARIM, airMax.getId()));

        // THEN
        assertThat(unchanged).isSameAs(airMax);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product is a draft")
    void shouldSaveNothingWhenProductIsDraft() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM));

        // WHEN
        Throwable error = catchThrowable(() -> unpublishProductService.unpublishProduct(commandOf(KARIM, airMax.getId())));

        // THEN
        assertThat(error).isInstanceOf(ProductNotPublishedException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product belongs to another seller")
    void shouldSaveNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish());

        // WHEN
        Throwable error = catchThrowable(() -> unpublishProductService.unpublishProduct(commandOf(LEA, airMax.getId())));

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
        Throwable error = catchThrowable(() -> unpublishProductService.unpublishProduct(commandOf(KARIM, unknown)));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD announce nothing WHEN the product changed before it could be saved")
    void shouldAnnounceNothingWhenProductChangedBeforeBeingSaved() {
        // GIVEN
        Product airMax = stored(draftWithVariant(KARIM).publish());
        doThrow(new ProductModifiedConcurrentlyException(airMax.getId())).when(productRepository).save(any());

        // WHEN
        Throwable error = catchThrowable(() -> unpublishProductService.unpublishProduct(commandOf(KARIM, airMax.getId())));

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        verifyNoInteractions(productEventPublisher);
    }

    private Product stored(final Product product) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private static Product draftWithVariant(final SellerId sellerId) {
        return Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now())
                .addVariant(Variant.create("42", "Rouge", Money.euros(16_000)));
    }

    private static UnpublishProductCommand commandOf(final SellerId sellerId, final ProductId productId) {
        return UnpublishProductCommand.builder().sellerId(sellerId).productId(productId).build();
    }
}
