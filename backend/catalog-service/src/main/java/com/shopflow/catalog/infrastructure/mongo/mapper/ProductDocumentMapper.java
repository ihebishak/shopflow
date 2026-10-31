package com.shopflow.catalog.infrastructure.mongo.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import org.mapstruct.Mapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Conversions entre le domaine et les documents MongoDB.
 * <p>
 * Produit → document : généré par MapStruct ({@code ProductDocumentMapperImpl}).
 * Document → produit : écrit à la main, car le domaine cache son constructeur et ne se reconstruit
 * que par {@link Product#restore}, que MapStruct ne sait pas appeler seul.
 */
@Mapper
public interface ProductDocumentMapper {

    ProductDocument toDocument(Product product);

    default Product toDomain(final ProductDocument document) {
        return Product.restore(new ProductId(UUID.fromString(document.getId())), new SellerId(document.getSellerId()),
                document.getName(), document.getBrand(), document.getCategory(), document.getDescription(),
                document.getStatus(), document.getCreatedAt());
    }

    default String map(final ProductId productId) {
        return productId.toString();
    }

    default String map(final SellerId sellerId) {
        return sellerId.getValue();
    }

    default String map(final Optional<String> value) {
        return value.orElse(null);
    }
}
