package com.shopflow.catalog.infrastructure.mongo;

import com.shopflow.catalog.application.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adaptateur MongoDB du port {@link ProductRepository} : convertit {@link Product} ↔ {@link ProductDocument}.
 * Participe automatiquement à la transaction en cours (@Transactional du cas d'usage).
 */
@Repository
public class MongoProductRepository implements ProductRepository {

    private final MongoTemplate mongoTemplate;

    public MongoProductRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Product product) {
        mongoTemplate.save(ProductDocument.from(product));
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(mongoTemplate.findById(id.toString(), ProductDocument.class))
                .map(ProductDocument::toDomain);
    }
}
