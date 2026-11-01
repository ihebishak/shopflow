package com.shopflow.catalog.infrastructure.web.swagger.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code CreateProductRequestDto}, pour que le DTO reste lisible.
 */
public interface CreateProductRequestDtoDocumentation {

    @Schema(description = "Nom du produit.", example = "Air Max 90 Infrared")
    String getName();

    @Schema(description = "Marque.", example = "Nike")
    String getBrand();

    @Schema(description = "Catégorie (l'arborescence viendra plus tard).", example = "sneakers")
    String getCategory();

    @Schema(description = "Description, facultative.", example = "La réédition du modèle de 1990",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String getDescription();
}
