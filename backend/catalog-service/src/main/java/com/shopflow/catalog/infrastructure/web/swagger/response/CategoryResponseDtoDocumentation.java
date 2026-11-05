package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation OpenAPI des champs de {@code CategoryResponseDto}, pour que le DTO reste lisible.
 */
public interface CategoryResponseDtoDocumentation {

    @Schema(description = "Identifiant de la catégorie.", example = "sneakers")
    String getSlug();

    @Schema(description = "Nom affiché.", example = "Sneakers")
    String getName();

    @Schema(description = "Identifiants des parents, du plus haut au plus proche ; vide au premier niveau.",
            example = "[\"chaussures\"]")
    List<String> getAncestors();
}
