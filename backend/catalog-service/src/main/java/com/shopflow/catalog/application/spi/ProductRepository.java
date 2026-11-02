package com.shopflow.catalog.application.spi;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductPage;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;

import java.util.Optional;

/**
 * Port de sortie : enregistrer et retrouver des produits. Les cas d'usage ne savent pas comment ;
 * l'implémentation (MongoDB) est un adaptateur de la couche infrastructure.
 */
public interface ProductRepository {

    /**
     * Crée le produit, ou le remplace s'il existe déjà.
     *
     * @throws com.shopflow.catalog.domain.ProductModifiedConcurrentlyException si le produit a changé depuis sa lecture
     */
    void save(Product product);

    Optional<Product> findById(ProductId id);

    /**
     * Une page des produits d'un vendeur, les plus récents d'abord.
     *
     * @param status le statut recherché, {@code null} pour tous les statuts
     * @param page   le numéro de la page, à partir de 0
     */
    ProductPage findBySeller(SellerId sellerId, ProductStatus status, int page, int size);
}
