package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.Product;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur crée un produit, qui naît en brouillon.
 */
public interface CreateProductUseCase {

    /**
     * @throws ConstraintViolationException si la commande est invalide (rien n'est enregistré)
     */
    Product createProduct(@Valid CreateProductCommand command);
}
