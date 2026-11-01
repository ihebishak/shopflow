package com.shopflow.catalog.infrastructure.web.swagger.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code AddVariantRequestDto}, pour que le DTO reste lisible.
 */
public interface AddVariantRequestDtoDocumentation {

    @Schema(description = "Taille, en texte libre.", example = "42")
    String getSize();

    @Schema(description = "Coloris, en texte libre.", example = "Rouge")
    String getColor();

    @Schema(description = "Prix de la variante.")
    MoneyRequestDtoDocumentation getPrice();
}
