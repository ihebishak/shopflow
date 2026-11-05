package com.shopflow.catalog.infrastructure.web.swagger.request;

import com.shopflow.catalog.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code UpdateProductRequestDto}, pour que le DTO reste lisible.
 */
public interface UpdateProductRequestDtoDocumentation {

    @Schema(description = "Nom du produit.", example = "Air Max 90 Infrared")
    String getName();

    @Schema(description = "Marque.", example = "Nike")
    String getBrand();

    @Schema(description = "Identifiant d'une catégorie de l'arbre (GET /api/v1/categories), à n'importe quel niveau.",
            example = "sneakers")
    String getCategory();

    @Schema(description = "À qui s'adresse le produit.", example = "UNISEX")
    Gender getGender();

    @Schema(description = "Description, facultative : absente, elle efface celle qui existait.",
            example = "La réédition du modèle de 1990", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String getDescription();
}
