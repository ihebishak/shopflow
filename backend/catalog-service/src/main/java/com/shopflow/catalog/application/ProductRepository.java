package com.shopflow.catalog.application;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;

import java.util.Optional;

/**
 * Port : enregistrer et retrouver des produits. Le cas d'usage ne sait pas comment ;
 * l'implémentation (MongoDB) est un adaptateur de la couche infrastructure.
 */
public interface ProductRepository {

    /** Crée le produit, ou le remplace s'il existe déjà. */
    void save(Product product);

    Optional<Product> findById(ProductId id);
}
