package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;

/**
 * La catégorie parente est déjà au dernier niveau de l'arbre.
 */
public class CategoryTooDeepException extends BusinessException {

    public static final String CODE = "CATEGORY_TOO_DEEP";

    public CategoryTooDeepException(final CategorySlug parent) {
        super(ErrorKind.CONFLICT, CODE, "Category " + parent + " cannot have children: the tree is limited to "
                + Category.MAX_DEPTH + " levels");
    }
}
