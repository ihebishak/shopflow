package com.shopflow.catalog.infrastructure.mongo.repository;

import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adaptateur MongoDB du port {@link ProductRepository} : convertit (mapper), puis lit ou écrit.
 * Participe automatiquement à la transaction en cours (@Transactional du cas d'usage).
 */
@Repository
@RequiredArgsConstructor
public class MongoProductRepository implements ProductRepository {

    private final MongoTemplate mongoTemplate;
    private final ProductDocumentMapper productDocumentMapper;

    @Override
    public void save(final Product product) {
        mongoTemplate.save(productDocumentMapper.toDocument(product));
    }

    @Override
    public Optional<Product> findById(final ProductId id) {
        return Optional.ofNullable(mongoTemplate.findById(id.toString(), ProductDocument.class))
                .map(productDocumentMapper::toDomain);
    }
}
