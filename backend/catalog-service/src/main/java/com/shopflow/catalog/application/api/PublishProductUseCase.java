package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.PublishProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.ProductWithoutVariantException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur met en vente un de ses produits.
 */
public interface PublishProductUseCase {

    /**
     * @return le produit publié, identique à celui enregistré s'il l'était déjà
     * @throws ConstraintViolationException         si la commande est invalide (rien n'est enregistré)
     * @throws ProductNotFoundException             si le produit n'existe pas ou appartient à un autre vendeur
     * @throws ProductWithoutVariantException       si le produit n'a aucune variante
     * @throws ProductModifiedConcurrentlyException si le produit a changé pendant la publication
     */
    Product publishProduct(@Valid PublishProductCommand command);
}
