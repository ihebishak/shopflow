package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.ChangeVariantPriceCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.domain.VariantNotFoundException;
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
class ChangeVariantPriceServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");
    private static final Money OLD_PRICE = Money.euros(16_000);
    private static final Money NEW_PRICE = Money.euros(12_900);

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductEventPublisher productEventPublisher = mock(ProductEventPublisher.class);
    private final ChangeVariantPriceService changeVariantPriceService = new ChangeVariantPriceService(
            new GetSellerProductService(productRepository), productRepository, productEventPublisher);

    @Test
    @DisplayName("SHOULD save the new price then announce both prices WHEN the price changes")
    void shouldSaveNewPriceThenAnnounceBothPricesWhenPriceChanges() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", OLD_PRICE);
        Product airMax = existingProductOf(KARIM, variant);

        // WHEN
        Variant changed = changeVariantPriceService.changeVariantPrice(
                commandOf(KARIM, airMax.getId(), variant.getSku(), NEW_PRICE));

        // THEN
        assertThat(changed.getSku()).isEqualTo(variant.getSku());
        assertThat(changed.getPrice()).isEqualTo(NEW_PRICE);
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(saved.capture());
        saveThenPublish.verify(productEventPublisher)
                .publishProductPriceChanged(airMax.getId(), variant.getSku(), OLD_PRICE, NEW_PRICE);
        assertThat(saved.getValue().variant(variant.getSku()).getPrice()).isEqualTo(NEW_PRICE);
    }

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN the price is the same")
    void shouldSaveAndAnnounceNothingWhenPriceIsTheSame() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", OLD_PRICE);
        Product airMax = existingProductOf(KARIM, variant);

        // WHEN
        Variant unchanged = changeVariantPriceService.changeVariantPrice(
                commandOf(KARIM, airMax.getId(), variant.getSku(), OLD_PRICE));

        // THEN
        assertThat(unchanged).isSameAs(variant);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product belongs to another seller")
    void shouldSaveNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", OLD_PRICE);
        Product airMax = existingProductOf(KARIM, variant);

        // WHEN
        Throwable error = catchThrowable(() -> changeVariantPriceService.changeVariantPrice(
                commandOf(LEA, airMax.getId(), variant.getSku(), NEW_PRICE)));

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
        Throwable error = catchThrowable(() -> changeVariantPriceService.changeVariantPrice(
                commandOf(KARIM, unknown, Sku.newSku(), NEW_PRICE)));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product has no variant with this sku")
    void shouldSaveNothingWhenProductHasNoVariantWithThisSku() {
        // GIVEN
        Product airMax = existingProductOf(KARIM, Variant.create("42", "Rouge", OLD_PRICE));

        // WHEN
        Throwable error = catchThrowable(() -> changeVariantPriceService.changeVariantPrice(
                commandOf(KARIM, airMax.getId(), Sku.newSku(), NEW_PRICE)));

        // THEN
        assertThat(error).isInstanceOf(VariantNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD announce nothing WHEN the product changed before it could be saved")
    void shouldAnnounceNothingWhenProductChangedBeforeBeingSaved() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", OLD_PRICE);
        Product airMax = existingProductOf(KARIM, variant);
        doThrow(new ProductModifiedConcurrentlyException(airMax.getId())).when(productRepository).save(any());

        // WHEN
        Throwable error = catchThrowable(() -> changeVariantPriceService.changeVariantPrice(
                commandOf(KARIM, airMax.getId(), variant.getSku(), NEW_PRICE)));

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        verifyNoInteractions(productEventPublisher);
    }

    private Product existingProductOf(final SellerId sellerId, final Variant variant) {
        Product product = Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now())
                .addVariant(variant);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private static ChangeVariantPriceCommand commandOf(final SellerId sellerId, final ProductId productId,
                                                       final Sku sku, final Money price) {
        return ChangeVariantPriceCommand.builder().sellerId(sellerId).productId(productId).sku(sku).price(price)
                .build();
    }
}
