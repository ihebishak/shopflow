package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.UpdateProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur modifie les informations d'un de ses produits.
 */
public interface UpdateProductUseCase {

    /**
     * @return le produit à jour, identique à celui enregistré si rien n'a changé
     * @throws ConstraintViolationException         si la commande est invalide (rien n'est enregistré)
     * @throws ProductNotFoundException             si le produit n'existe pas ou appartient à un autre vendeur
     * @throws ProductModifiedConcurrentlyException si le produit a changé pendant la modification
     */
    Product updateProduct(@Valid UpdateProductCommand command);
}
