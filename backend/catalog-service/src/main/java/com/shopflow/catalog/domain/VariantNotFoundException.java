package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Le produit n'a pas de variante avec ce SKU : elle n'existe pas, ou appartient à un autre produit.
 */
public class VariantNotFoundException extends BusinessException {

    public static final String CODE = "VARIANT_NOT_FOUND";

    public VariantNotFoundException(final ProductId productId, final Sku sku) {
        super(ErrorKind.NOT_FOUND, CODE, "Variant " + sku + " not found on product " + productId);
    }
}
