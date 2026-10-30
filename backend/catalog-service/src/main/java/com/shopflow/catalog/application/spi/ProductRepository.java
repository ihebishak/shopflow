package com.shopflow.catalog.application.spi;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;

import java.util.Optional;

/**
 * Port de sortie : enregistrer et retrouver des produits. Les cas d'usage ne savent pas comment ;
 * l'implémentation (MongoDB) est un adaptateur de la couche infrastructure.
 */
public interface ProductRepository {

    /** Crée le produit, ou le remplace s'il existe déjà. */
    void save(Product product);

    Optional<Product> findById(ProductId id);
}
