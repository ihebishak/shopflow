package com.shopflow.catalog.application.api;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;

/**
 * Port d'entrée : un vendeur consulte un de ses produits.
 */
public interface GetSellerProductUseCase {

    /**
     * @throws ProductNotFoundException si le produit n'existe pas ou appartient à un autre vendeur
     */
    Product getSellerProduct(SellerId sellerId, ProductId productId);
}
