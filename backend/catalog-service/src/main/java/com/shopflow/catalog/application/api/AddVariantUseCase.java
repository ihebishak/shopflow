package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.domain.DuplicateVariantException;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.Variant;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur ajoute une variante à l'un de ses produits.
 */
public interface AddVariantUseCase {

    /**
     * @return la variante créée, avec son nouveau SKU
     * @throws ConstraintViolationException          si la commande est invalide (rien n'est enregistré)
     * @throws ProductNotFoundException              si le produit n'existe pas ou appartient à un autre vendeur
     * @throws DuplicateVariantException             si le produit a déjà cette taille et ce coloris
     * @throws ProductModifiedConcurrentlyException  si le produit a changé pendant l'ajout
     */
    Variant addVariant(@Valid AddVariantCommand command);
}
