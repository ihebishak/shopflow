package com.shopflow.catalog.domain;

/**
 * Une règle métier d'un produit n'est pas respectée (champ manquant, trop long…).
 */
public class InvalidProductException extends RuntimeException {

    private final String field;

    public InvalidProductException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** Champ en cause, pour l'indiquer au client de l'API. */
    public String field() {
        return field;
    }
}
