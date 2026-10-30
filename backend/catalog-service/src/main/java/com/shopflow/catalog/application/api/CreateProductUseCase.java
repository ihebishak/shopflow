package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.InvalidProductException;
import com.shopflow.catalog.domain.Product;

/**
 * Port d'entrée : un vendeur crée un produit, qui naît en brouillon.
 */
public interface CreateProductUseCase {

    /**
     * @throws InvalidProductException si une règle n'est pas respectée (rien n'est enregistré)
     */
    Product createProduct(CreateProductCommand command);
}
