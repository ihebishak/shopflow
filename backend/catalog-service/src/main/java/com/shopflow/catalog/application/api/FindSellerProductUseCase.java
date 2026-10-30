package com.shopflow.catalog.application.api;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;

import java.util.Optional;

/**
 * Port d'entrée : un vendeur consulte un de ses produits.
 */
public interface FindSellerProductUseCase {

    /**
     * @return vide si le produit n'existe pas ou appartient à un autre vendeur (son existence n'est pas révélée)
     */
    Optional<Product> findSellerProduct(SellerId sellerId, ProductId productId);
}
