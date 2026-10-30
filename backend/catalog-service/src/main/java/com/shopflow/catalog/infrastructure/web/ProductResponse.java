package com.shopflow.catalog.infrastructure.web;

import com.shopflow.catalog.domain.Product;

import java.time.Instant;

/**
 * Produit renvoyé par l'API vendeur. Séparé du domaine : le format de l'API reste stable si le domaine évolue.
 *
 * @param description {@code null} s'il n'y en a pas
 */
record ProductResponse(
        String id,
        String sellerId,
        String name,
        String brand,
        String category,
        String description,
        String status,
        Instant createdAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId().toString(), product.getSellerId().getValue(), product.getName(),
                product.getBrand(), product.getCategory(), product.getDescription().orElse(null),
                product.getStatus().name(), product.getCreatedAt());
    }
}
