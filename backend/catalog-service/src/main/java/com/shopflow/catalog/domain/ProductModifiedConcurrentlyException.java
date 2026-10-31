package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Le produit a changé depuis sa lecture : l'enregistrer écraserait cette modification. Il suffit de réessayer.
 */
public class ProductModifiedConcurrentlyException extends BusinessException {

    public static final String CODE = "PRODUCT_MODIFIED_CONCURRENTLY";

    public ProductModifiedConcurrentlyException(final ProductId productId) {
        super(ErrorKind.CONFLICT, CODE, "Product " + productId + " was modified concurrently");
    }
}
