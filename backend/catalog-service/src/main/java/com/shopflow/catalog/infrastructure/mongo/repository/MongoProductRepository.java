package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.MongoException;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductPage;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Adaptateur MongoDB du port {@link ProductRepository} : convertit (mapper), puis lit ou écrit.
 * Participe automatiquement à la transaction en cours (@Transactional du cas d'usage).
 */
@Repository
@RequiredArgsConstructor
public class MongoProductRepository implements ProductRepository {

    /** Une autre transaction a modifié le document : MongoDB refuse l'écriture avant que la version soit comparée. */
    private static final int WRITE_CONFLICT = 112;

    /** {@code _id} départage les produits créés au même instant : l'ordre reste le même d'une page à l'autre. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "_id");

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

    @Override
    public ProductPage findBySeller(final SellerId sellerId, final ProductStatus status, final int page,
                                    final int size) {
        Query query = query(where("sellerId").is(sellerId.getValue()));
        if (status != null) {
            query.addCriteria(where("status").is(status));
        }
        long totalElements = mongoTemplate.count(query, ProductDocument.class);
        List<Product> content = mongoTemplate
                .find(query.with(PageRequest.of(page, size, NEWEST_FIRST)), ProductDocument.class).stream()
                .map(productDocumentMapper::toDomain).toList();
        return ProductPage.builder().content(content).page(page).size(size).totalElements(totalElements).build();
    }

    private static boolean isWriteConflict(final DataIntegrityViolationException exception) {
        return exception.getCause() instanceof MongoException mongoException
                && mongoException.getCode() == WRITE_CONFLICT;
    }
}
