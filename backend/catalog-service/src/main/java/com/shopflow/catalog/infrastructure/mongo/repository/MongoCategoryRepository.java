package com.shopflow.catalog.infrastructure.mongo.repository;

import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryAlreadyExistsException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.mongo.document.CategoryDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.CategoryDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptateur MongoDB du port {@link CategoryRepository} : convertit (mapper), puis lit ou écrit.
 */
@Repository
@RequiredArgsConstructor
public class MongoCategoryRepository implements CategoryRepository {

    private static final Sort BY_SLUG = Sort.by("_id");

    private final MongoTemplate mongoTemplate;
    private final CategoryDocumentMapper categoryDocumentMapper;

    @Override
    public void create(final Category category) {
        try {
            mongoTemplate.insert(categoryDocumentMapper.toDocument(category));
        } catch (DuplicateKeyException exception) {
            throw new CategoryAlreadyExistsException(category.getSlug());
        }
    }

    @Override
    public Optional<Category> findBySlug(final CategorySlug slug) {
        return Optional.ofNullable(mongoTemplate.findById(slug.getValue(), CategoryDocument.class))
                .map(categoryDocumentMapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return mongoTemplate.find(new Query().with(BY_SLUG), CategoryDocument.class).stream()
                .map(categoryDocumentMapper::toDomain).toList();
    }
}
