package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation OpenAPI des champs de {@code CategoryNodeResponseDto}, pour que le DTO reste lisible.
 */
public interface CategoryNodeResponseDtoDocumentation {

    @Schema(description = "Identifiant de la catégorie.", example = "chaussures")
    String getSlug();

    @Schema(description = "Nom affiché.", example = "Chaussures")
    String getName();

    @Schema(description = "Les sous-catégories ; liste vide s'il n'y en a pas.")
    List<? extends CategoryNodeResponseDtoDocumentation> getChildren();
}
