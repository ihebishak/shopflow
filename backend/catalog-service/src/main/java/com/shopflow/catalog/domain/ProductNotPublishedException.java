package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Un brouillon ne peut pas être dépublié : il n'a jamais été en vente.
 */
public class ProductNotPublishedException extends BusinessException {

    public static final String CODE = "PRODUCT_NOT_PUBLISHED";

    public ProductNotPublishedException(final ProductId productId) {
        super(ErrorKind.CONFLICT, CODE, "Product " + productId + " is not published");
    }
}
