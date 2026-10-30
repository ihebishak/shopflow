package com.shopflow.catalog.domain;

import com.shopflow.kernel.id.UuidV7;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifiant d'un produit (UUID v7). Un type dédié empêche de le confondre avec un autre identifiant.
 */
@Getter
@EqualsAndHashCode
public final class ProductId {

    private final UUID value;

    public ProductId(final UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static ProductId newId() {
        return new ProductId(UuidV7.generate());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
