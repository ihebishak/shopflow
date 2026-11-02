package com.shopflow.catalog.infrastructure.web.swagger.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code CreateCategoryRequestDto}, pour que le DTO reste lisible.
 */
public interface CreateCategoryRequestDtoDocumentation {

    @Schema(description = "Identifiant de la catégorie : minuscules, chiffres et tirets simples.", example = "sneakers")
    String getSlug();

    @Schema(description = "Nom affiché.", example = "Sneakers")
    String getName();

    @Schema(description = "Identifiant de la catégorie parente ; absent pour une catégorie de premier niveau.",
            example = "chaussures", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String getParentSlug();
}
