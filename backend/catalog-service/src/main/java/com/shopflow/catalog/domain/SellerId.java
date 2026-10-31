package com.shopflow.catalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Identifiant du vendeur propriétaire d'un produit. Un type dédié empêche de le confondre avec un autre identifiant.
 */
@Getter
@EqualsAndHashCode
public final class SellerId {

    private final String value;

    /**
     * @throws InvalidSellerIdException si l'identifiant est vide
     */
    public SellerId(final String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new InvalidSellerIdException();
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
