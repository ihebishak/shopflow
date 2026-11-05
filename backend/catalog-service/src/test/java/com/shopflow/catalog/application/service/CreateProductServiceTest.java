package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.Gender;
import com.shopflow.catalog.domain.InvalidCategorySlugException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.UnpublishReason;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Le cas d'usage avec de faux adaptateurs en mémoire : ni MongoDB, ni Kafka, ni Spring.
 */
class CreateProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final SellerId KARIM = new SellerId("seller-karim");

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private final RecordingProductEventPublisher productEventPublisher = new RecordingProductEventPublisher();
    private final CreateProductService createProductService = new CreateProductService(productRepository,
            categoryRepository, productEventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void createCategoryTree() {
        Category shoes = Category.root(new CategorySlug("chaussures"), "Chaussures");
        categoryRepository.create(shoes);
        categoryRepository.create(shoes.child(new CategorySlug("sneakers"), "Sneakers"));
    }

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

    @Test
    @DisplayName("SHOULD accept a first-level category WHEN it exists in the tree")
    void shouldAcceptFirstLevelCategoryWhenItExists() {
        // WHEN
        Product product = createProductService.createProduct(airMaxCommandIn("chaussures"));

        // THEN
        assertThat(product.getCategory()).isEqualTo("chaussures");
    }

    @Test
    @DisplayName("SHOULD create and announce nothing WHEN the category does not exist")
    void shouldCreateAndAnnounceNothingWhenCategoryDoesNotExist() {
        // WHEN
        Throwable error = catchThrowable(() -> createProductService.createProduct(airMaxCommandIn("snekers")));

        // THEN
        assertThat(error).isInstanceOf(CategoryNotFoundException.class);
        assertThat(productEventPublisher.announced).isEmpty();
    }

    @Test
    @DisplayName("SHOULD create and announce nothing WHEN the category is badly formed")
    void shouldCreateAndAnnounceNothingWhenCategoryIsBadlyFormed() {
        // WHEN
        Throwable error = catchThrowable(() -> createProductService.createProduct(airMaxCommandIn("Sneakers")));

        // THEN
        assertThat(error).isInstanceOf(InvalidCategorySlugException.class);
        assertThat(productEventPublisher.announced).isEmpty();
    }

    private static CreateProductCommand airMaxCommand(final String name) {
        return CreateProductCommand.builder().sellerId(KARIM).name(name).brand("Nike").category("sneakers")
                .gender(Gender.UNISEX).build();
    }

    private static CreateProductCommand airMaxCommandIn(final String category) {
        return CreateProductCommand.builder().sellerId(KARIM).name("Air Max 90 Infrared").brand("Nike")
                .category(category).gender(Gender.UNISEX).build();
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

        @Override
        public void publishProductUpdated(final ProductId productId, final List<String> changedFields) {
            throw new UnsupportedOperationException("creating a product updates nothing");
        }

        @Override
        public void publishProductPriceChanged(final ProductId productId, final Sku sku, final Money oldPrice,
                                               final Money newPrice) {
            throw new UnsupportedOperationException("creating a product changes no price");
        }

        @Override
        public void publishProductPublished(final Product product) {
            throw new UnsupportedOperationException("creating a product publishes nothing");
        }

        @Override
        public void publishProductUnpublished(final Product product, final UnpublishReason reason) {
            throw new UnsupportedOperationException("creating a product unpublishes nothing");
        }
    }
}
