package com.shopflow.catalog.application.api;

import com.shopflow.catalog.domain.CategoryNode;

import java.util.List;

/**
 * Port d'entrée : n'importe qui lit l'arbre des catégories.
 */
public interface GetCategoryTreeUseCase {

    /**
     * @return les catégories de premier niveau, chacune avec ses descendants
     */
    List<CategoryNode> getCategoryTree();
}
