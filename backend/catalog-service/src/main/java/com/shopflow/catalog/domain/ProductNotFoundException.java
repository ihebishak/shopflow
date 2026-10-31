package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Le produit n'existe pas, ou appartient à un autre vendeur (son existence n'est pas révélée).
 */
public class ProductNotFoundException extends BusinessException {

    public static final String CODE = "PRODUCT_NOT_FOUND";

    public ProductNotFoundException(final ProductId productId) {
        super(ErrorKind.NOT_FOUND, CODE, "Product " + productId + " not found");
    }
}
