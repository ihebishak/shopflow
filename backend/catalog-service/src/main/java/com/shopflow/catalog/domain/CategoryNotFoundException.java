package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Aucune catégorie ne porte cet identifiant.
 */
public class CategoryNotFoundException extends BusinessException {

    public static final String CODE = "CATEGORY_NOT_FOUND";

    public CategoryNotFoundException(final CategorySlug slug) {
        super(ErrorKind.NOT_FOUND, CODE, "Category " + slug + " not found");
    }
}
