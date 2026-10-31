package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.MongoException;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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

    /** Une autre transaction a modifié le document : MongoDB refuse l'écriture avant que la version soit comparée. */
    private static final int WRITE_CONFLICT = 112;

    private final MongoTemplate mongoTemplate;
    private final ProductDocumentMapper productDocumentMapper;

    @Override
    public void save(final Product product) {
        try {
            mongoTemplate.save(productDocumentMapper.toDocument(product));
        } catch (OptimisticLockingFailureException exception) {
            throw new ProductModifiedConcurrentlyException(product.getId());
        } catch (DataIntegrityViolationException exception) {
            if (isWriteConflict(exception)) {
                throw new ProductModifiedConcurrentlyException(product.getId());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Product> findById(final ProductId id) {
        return Optional.ofNullable(mongoTemplate.findById(id.toString(), ProductDocument.class))
                .map(productDocumentMapper::toDomain);
    }

    private static boolean isWriteConflict(final DataIntegrityViolationException exception) {
        return exception.getCause() instanceof MongoException mongoException
                && mongoException.getCode() == WRITE_CONFLICT;
    }
}
