package com.shopflow.catalog.domain;

import java.util.Objects;

/**
 * Identifiant du vendeur propriétaire d'un produit. Un type dédié empêche de le confondre avec un autre identifiant.
 */
public record SellerId(String value) {

    public SellerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Seller id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
