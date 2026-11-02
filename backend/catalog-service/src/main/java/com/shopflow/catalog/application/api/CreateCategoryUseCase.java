package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.domain.Category;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : l'admin ajoute une catégorie à l'arbre.
 */
public interface CreateCategoryUseCase {

    /**
     * @throws ConstraintViolationException si la commande est invalide (rien n'est enregistré)
     * @throws com.shopflow.catalog.domain.CategoryNotFoundException si le parent n'existe pas
     * @throws com.shopflow.catalog.domain.CategoryTooDeepException si le parent est au dernier niveau de l'arbre
     * @throws com.shopflow.catalog.domain.CategoryAlreadyExistsException si l'identifiant est déjà pris
     */
    Category createCategory(@Valid CreateCategoryCommand command);
}
