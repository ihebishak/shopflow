package com.shopflow.catalog.domain;

import com.shopflow.kernel.id.UuidV7;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifiant d'un produit (UUID v7). Un type dédié empêche de le confondre avec un autre identifiant.
 */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "value");
    }

    public static ProductId newId() {
        return new ProductId(UuidV7.generate());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
