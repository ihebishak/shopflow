package com.shopflow.catalog.application;

import com.shopflow.catalog.domain.InvalidProductException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le cas d'usage avec un faux dépôt en mémoire : ni MongoDB ni Spring, quelques millisecondes.
 */
class CreateProductTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final SellerId KARIM = new SellerId("seller-karim");

    private final InMemoryProducts products = new InMemoryProducts();
    private final CreateProduct createProduct = new CreateProduct(products, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void creates_a_draft_and_saves_it() {
        Product product = createProduct.handle(
                new CreateProduct.Command(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null));

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.createdAt()).as("heure de l'horloge").isEqualTo(NOW);
        assertThat(products.findById(product.id())).contains(product);
    }

    @Test
    void saves_nothing_when_a_rule_is_broken() {
        assertThatThrownBy(() -> createProduct.handle(new CreateProduct.Command(KARIM, " ", "Nike", "sneakers", null)))
                .isInstanceOf(InvalidProductException.class);

        assertThat(products.stored).isEmpty();
    }

    private static final class InMemoryProducts implements ProductRepository {

        final Map<ProductId, Product> stored = new HashMap<>();

        @Override
        public void save(Product product) {
            stored.put(product.id(), product);
        }

        @Override
        public Optional<Product> findById(ProductId id) {
            return Optional.ofNullable(stored.get(id));
        }
    }
}
