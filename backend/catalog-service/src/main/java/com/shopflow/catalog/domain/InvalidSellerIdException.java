package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * L'identifiant du vendeur est vide.
 */
public class InvalidSellerIdException extends BusinessException {

    public static final String CODE = "INVALID_SELLER_ID";

    public InvalidSellerIdException() {
        super(ErrorKind.INVALID, CODE, "Seller id must not be blank");
    }
}
