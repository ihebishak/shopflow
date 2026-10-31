package com.shopflow.catalog.infrastructure.mongo.document;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Forme d'un produit dans MongoDB (collection {@code products}).
 * Séparée de {@link Product} pour que le domaine reste sans annotations MongoDB.
 */
@Getter
@Builder
@AllArgsConstructor
@Document(ProductDocument.COLLECTION)
public class ProductDocument {

    public static final String COLLECTION = "products";

    /** UUID v7 du produit, en texte. */
    @Id
    private final String id;

    private final String sellerId;
    private final String name;
    private final String brand;
    private final String category;

    /** {@code null} s'il n'y en a pas : le champ est alors absent du document. */
    private final String description;

    private final ProductStatus status;
    private final Instant createdAt;
}
