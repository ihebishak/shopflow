package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.UnpublishProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.ProductNotPublishedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur retire de la vente un de ses produits.
 */
public interface UnpublishProductUseCase {

    /**
     * @return le produit dépublié, identique à celui enregistré s'il l'était déjà
     * @throws ConstraintViolationException         si la commande est invalide (rien n'est enregistré)
     * @throws ProductNotFoundException             si le produit n'existe pas ou appartient à un autre vendeur
     * @throws ProductNotPublishedException         si le produit est encore un brouillon
     * @throws ProductModifiedConcurrentlyException si le produit a changé pendant la dépublication
     */
    Product unpublishProduct(@Valid UnpublishProductCommand command);
}
