package com.shopflow.catalog.infrastructure.mongo.document;

import com.shopflow.catalog.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Forme d'une catégorie dans MongoDB (collection {@code categories}).
 * Séparée de {@link Category} pour que le domaine reste sans annotations MongoDB.
 */
@Getter
@Builder
@AllArgsConstructor
@Document(CategoryDocument.COLLECTION)
public class CategoryDocument {

    public static final String COLLECTION = "categories";

    /** Stocké dans {@code _id} : MongoDB garantit qu'un identifiant n'existe qu'une fois. */
    @Id
    private final String slug;

    private final String name;

    /** Identifiants des parents, du plus haut au plus proche. */
    private final List<String> ancestors;
}
