package com.shopflow.catalog.application.spi;

import com.shopflow.catalog.domain.Product;

/**
 * Port de sortie : annoncer aux autres services ce qui arrive aux produits. Les cas d'usage ne savent pas comment ;
 * l'implémentation (outbox + Kafka) est un adaptateur de la couche infrastructure.
 * À appeler dans la transaction du cas d'usage : l'annonce est enregistrée si et seulement si le produit l'est.
 */
public interface ProductEventPublisher {

    void publishProductCreated(Product product);
}
