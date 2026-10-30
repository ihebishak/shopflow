package com.shopflow.catalog.domain;

import lombok.Getter;

/**
 * Une règle métier d'un produit n'est pas respectée (champ manquant, trop long…).
 */
@Getter
public class InvalidProductException extends RuntimeException {

    /** Champ en cause, pour l'indiquer au client de l'API. */
    private final String field;

    public InvalidProductException(final String field, final String message) {
        super(message);
        this.field = field;
    }
}
