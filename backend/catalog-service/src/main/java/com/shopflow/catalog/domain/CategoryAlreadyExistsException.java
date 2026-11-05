package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * Une catégorie porte déjà cet identifiant.
 */
public class CategoryAlreadyExistsException extends BusinessException {

    public static final String CODE = "CATEGORY_ALREADY_EXISTS";

    public CategoryAlreadyExistsException(final CategorySlug slug) {
        super(ErrorKind.CONFLICT, CODE, "Category " + slug + " already exists");
    }
}
