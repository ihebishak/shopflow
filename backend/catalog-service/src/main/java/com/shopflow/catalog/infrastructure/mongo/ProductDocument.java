package com.shopflow.catalog.infrastructure.mongo;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Forme d'un produit dans MongoDB (collection {@code products}).
 * Séparée de {@link Product} pour que le domaine reste du Java pur, sans annotations MongoDB.
 */
@Document(ProductDocument.COLLECTION)
record ProductDocument(
        @Id String id,
        String sellerId,
        String name,
        String brand,
        String category,
        String description,
        ProductStatus status,
        Instant createdAt) {

    static final String COLLECTION = "products";

    static ProductDocument from(Product product) {
        return new ProductDocument(product.id().toString(), product.sellerId().value(), product.name(),
                product.brand(), product.category(), product.description().orElse(null), product.status(),
                product.createdAt());
    }

    Product toDomain() {
        return Product.restore(new ProductId(UUID.fromString(id)), new SellerId(sellerId), name, brand, category,
                description, status, createdAt);
    }
}
