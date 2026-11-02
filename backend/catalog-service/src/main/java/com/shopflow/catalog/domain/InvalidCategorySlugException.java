package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * L'identifiant de la catégorie est trop long ou mal formé.
 */
public class InvalidCategorySlugException extends BusinessException {

    public static final String CODE = "INVALID_CATEGORY_SLUG";

    public InvalidCategorySlugException() {
        super(ErrorKind.INVALID, CODE, "Category slug must use lowercase letters, digits and single hyphens, "
                + "at most " + CategorySlug.MAX_LENGTH + " characters");
    }
}
