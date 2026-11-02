package com.shopflow.catalog.application.spi;

import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategorySlug;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie : enregistrer et retrouver des catégories. Les cas d'usage ne savent pas comment ;
 * l'implémentation (MongoDB) est un adaptateur de la couche infrastructure.
 */
public interface CategoryRepository {

    /**
     * Crée la catégorie, sans jamais remplacer une catégorie existante.
     *
     * @throws com.shopflow.catalog.domain.CategoryAlreadyExistsException si l'identifiant est déjà pris
     */
    void create(Category category);

    Optional<Category> findBySlug(CategorySlug slug);

    /** Toutes les catégories, triées par identifiant : l'ordre reste le même d'une lecture à l'autre. */
    List<Category> findAll();
}
