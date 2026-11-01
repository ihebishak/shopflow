package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Un produit sans variante ne peut pas être publié : les clients ne pourraient rien acheter.
 */
public class ProductWithoutVariantException extends BusinessException {

    public static final String CODE = "PRODUCT_WITHOUT_VARIANT";

    public ProductWithoutVariantException(final ProductId productId) {
        super(ErrorKind.CONFLICT, CODE, "Product " + productId + " has no variant");
    }
}
