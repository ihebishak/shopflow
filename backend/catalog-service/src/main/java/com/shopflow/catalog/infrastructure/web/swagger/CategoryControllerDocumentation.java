package com.shopflow.catalog.infrastructure.web.swagger;

import com.shopflow.catalog.infrastructure.web.dto.response.CategoryNodeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Documentation OpenAPI de l'API publique des catégories (page Swagger UI).
 * Portée par une interface pour que le contrôleur reste sans annotations de documentation.
 */
@Tag(name = "Catégories", description = "Tout le monde peut consulter l'arbre des catégories du catalogue.")
public interface CategoryControllerDocumentation {

    @Operation(summary = "Lire l'arbre des catégories",
            description = "Les catégories de premier niveau, chacune avec ses sous-catégories, triées par identifiant.")
    @ApiResponse(responseCode = "200", description = "L'arbre, vide s'il n'y a encore aucune catégorie.")
    List<CategoryNodeResponseDto> getCategoryTree();
}
