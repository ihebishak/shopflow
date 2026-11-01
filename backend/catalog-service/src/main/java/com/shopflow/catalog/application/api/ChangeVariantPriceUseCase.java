package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.command.ChangeVariantPriceCommand;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.domain.VariantNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur change le prix d'une variante d'un de ses produits.
 */
public interface ChangeVariantPriceUseCase {

    /**
     * @return la variante à son nouveau prix, identique à celle enregistrée si le prix n'a pas changé
     * @throws ConstraintViolationException         si la commande est invalide (rien n'est enregistré)
     * @throws ProductNotFoundException             si le produit n'existe pas ou appartient à un autre vendeur
     * @throws VariantNotFoundException             si le produit n'a pas de variante avec ce SKU
     * @throws ProductModifiedConcurrentlyException si le produit a changé pendant la modification
     */
    Variant changeVariantPrice(@Valid ChangeVariantPriceCommand command);
}
