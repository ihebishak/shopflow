package com.shopflow.catalog.infrastructure.mongo.mapper;

import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.mongo.document.CategoryDocument;
import org.mapstruct.Mapper;

/**
 * Conversions entre le domaine et les documents MongoDB.
 * <p>
 * Catégorie → document : généré par MapStruct ({@code CategoryDocumentMapperImpl}).
 * Document → catégorie : écrit à la main, car le domaine cache son constructeur et ne se reconstruit
 * que par {@link Category#restore}, que MapStruct ne sait pas appeler seul.
 */
@Mapper
public interface CategoryDocumentMapper {

    CategoryDocument toDocument(Category category);

    default Category toDomain(final CategoryDocument document) {
        return Category.restore(new CategorySlug(document.getSlug()), document.getName(),
                document.getAncestors().stream().map(CategorySlug::new).toList());
    }

    default String map(final CategorySlug slug) {
        return slug.getValue();
    }
}
