package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Le produit a déjà une variante avec cette taille et ce coloris.
 */
public class DuplicateVariantException extends BusinessException {

    public static final String CODE = "DUPLICATE_VARIANT";

    public DuplicateVariantException(final Variant variant) {
        super(ErrorKind.CONFLICT, CODE, "Variant " + variant.getSize() + " / " + variant.getColor() + " already exists");
    }
}
