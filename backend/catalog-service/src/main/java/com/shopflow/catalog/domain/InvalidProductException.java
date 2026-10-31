package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Une règle métier d'un produit n'est pas respectée (champ manquant, trop long…).
 */
public class InvalidProductException extends BusinessException {

    public static final String CODE = "INVALID_PRODUCT";

    public InvalidProductException(final String field, final String message) {
        super(ErrorKind.INVALID, CODE, field, message);
    }
}
