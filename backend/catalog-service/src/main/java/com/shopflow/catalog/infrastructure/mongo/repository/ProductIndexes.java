package com.shopflow.catalog.infrastructure.mongo.repository;

import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Crée au démarrage les index des produits d'un vendeur, avec ou sans filtre par statut : sans eux, MongoDB
 * parcourrait les produits de tous les vendeurs à chaque liste.
 */
@Component
@RequiredArgsConstructor
public class ProductIndexes implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        IndexOperations indexes = mongoTemplate.indexOps(ProductDocument.COLLECTION);
        indexes.createIndex(new Index().on("sellerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
                .on("_id", Sort.Direction.DESC).named("seller_newest_first"));
        indexes.createIndex(new Index().on("sellerId", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC)
                .named("seller_status_newest_first"));
    }
}
