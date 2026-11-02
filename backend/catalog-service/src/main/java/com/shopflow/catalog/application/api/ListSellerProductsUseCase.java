package com.shopflow.catalog.application.api;

import com.shopflow.catalog.application.api.query.ListSellerProductsQuery;
import com.shopflow.catalog.domain.ProductPage;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Port d'entrée : un vendeur liste ses produits, page par page.
 */
public interface ListSellerProductsUseCase {

    /**
     * @return la page demandée, les plus récents d'abord, vide si elle est au-delà de la fin
     * @throws ConstraintViolationException si la demande est invalide (rien n'est lu)
     */
    ProductPage listSellerProducts(@Valid ListSellerProductsQuery query);
}
