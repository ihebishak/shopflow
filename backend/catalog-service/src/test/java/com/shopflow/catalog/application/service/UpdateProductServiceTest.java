package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.UpdateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
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
class UpdateProductServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final SellerId LEA = new SellerId("seller-lea");

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private final ProductEventPublisher productEventPublisher = mock(ProductEventPublisher.class);
    private final UpdateProductService updateProductService = new UpdateProductService(
            new GetSellerProductService(productRepository), productRepository, categoryRepository,
            productEventPublisher);

    @BeforeEach
    void createCategoryTree() {
        Category shoes = Category.root(new CategorySlug("chaussures"), "Chaussures");
        categoryRepository.create(shoes);
        categoryRepository.create(shoes.child(new CategorySlug("sneakers"), "Sneakers"));
        categoryRepository.create(shoes.child(new CategorySlug("tennis"), "Tennis"));
    }

    @Test
    @DisplayName("SHOULD save the updated product then announce the changed fields WHEN an information changes")
    void shouldSaveUpdatedProductThenAnnounceChangedFieldsWhenInformationChanges() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Product updated = updateProductService.updateProduct(commandOf(KARIM, airMax.getId(), "Nouvelle description"));

        // THEN
        assertThat(updated.getDescription()).contains("Nouvelle description");
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        InOrder saveThenPublish = inOrder(productRepository, productEventPublisher);
        saveThenPublish.verify(productRepository).save(saved.capture());
        saveThenPublish.verify(productEventPublisher).publishProductUpdated(airMax.getId(), List.of("description"));
        assertThat(saved.getValue()).isSameAs(updated);
    }

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN nothing changes")
    void shouldSaveAndAnnounceNothingWhenNothingChanges() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Product updated = updateProductService.updateProduct(commandOf(KARIM, airMax.getId(), "Réédition"));

        // THEN
        assertThat(updated).isSameAs(airMax);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD save the product and announce its category WHEN it moves to an existing category")
    void shouldSaveProductAndAnnounceCategoryWhenItMovesToExistingCategory() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Product updated = updateProductService.updateProduct(categoryChangeOf(airMax.getId(), "tennis"));

        // THEN
        assertThat(updated.getCategory()).isEqualTo("tennis");
        verify(productRepository).save(updated);
        verify(productEventPublisher).publishProductUpdated(airMax.getId(), List.of("category"));
    }

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN the new category does not exist")
    void shouldSaveAndAnnounceNothingWhenNewCategoryDoesNotExist() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Throwable error = catchThrowable(
                () -> updateProductService.updateProduct(categoryChangeOf(airMax.getId(), "snekers")));

        // THEN
        assertThat(error).isInstanceOf(CategoryNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD not check the category WHEN it does not change")
    void shouldNotCheckCategoryWhenItDoesNotChange() {
        // GIVEN
        Product oldProduct = existingProductOf(KARIM, "streetwear");
        UpdateProductCommand command = UpdateProductCommand.builder().sellerId(KARIM).productId(oldProduct.getId())
                .name("Air Max 90 OG").brand("Nike").category("streetwear").description("Réédition").build();

        // WHEN
        Product updated = updateProductService.updateProduct(command);

        // THEN
        assertThat(updated.getName()).isEqualTo("Air Max 90 OG");
        verify(productRepository).save(updated);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the product belongs to another seller")
    void shouldSaveNothingWhenProductBelongsToAnotherSeller() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);

        // WHEN
        Throwable error = catchThrowable(
                () -> updateProductService.updateProduct(commandOf(LEA, airMax.getId(), "Nouvelle description")));

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
        Throwable error = catchThrowable(
                () -> updateProductService.updateProduct(commandOf(KARIM, unknown, "Nouvelle description")));

        // THEN
        assertThat(error).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD announce nothing WHEN the product changed before it could be saved")
    void shouldAnnounceNothingWhenProductChangedBeforeBeingSaved() {
        // GIVEN
        Product airMax = existingProductOf(KARIM);
        doThrow(new ProductModifiedConcurrentlyException(airMax.getId())).when(productRepository).save(any());

        // WHEN
        Throwable error = catchThrowable(
                () -> updateProductService.updateProduct(commandOf(KARIM, airMax.getId(), "Nouvelle description")));

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        verifyNoInteractions(productEventPublisher);
    }

    private Product existingProductOf(final SellerId sellerId) {
        return existingProductOf(sellerId, "sneakers");
    }

    /** Un produit peut avoir été rangé avant l'arbre, dans une catégorie qui n'y existe pas. */
    private Product existingProductOf(final SellerId sellerId, final String category) {
        Product product = Product.draft(sellerId, "Air Max 90 Infrared", "Nike", category, "Réédition", Instant.now());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private static UpdateProductCommand commandOf(final SellerId sellerId, final ProductId productId,
                                                  final String description) {
        return UpdateProductCommand.builder().sellerId(sellerId).productId(productId).name("Air Max 90 Infrared")
                .brand("Nike").category("sneakers").description(description).build();
    }

    private static UpdateProductCommand categoryChangeOf(final ProductId productId, final String category) {
        return UpdateProductCommand.builder().sellerId(KARIM).productId(productId).name("Air Max 90 Infrared")
                .brand("Nike").category(category).description("Réédition").build();
    }
}
