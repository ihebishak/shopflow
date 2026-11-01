package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code VariantResponseDto}, pour que le DTO reste lisible.
 */
public interface VariantResponseDtoDocumentation {

    @Schema(description = "Identifiant de la variante (UUID v7), généré par le serveur.",
            example = "01a19c2f-3c1d-7e20-8b4a-9d6e2f1a3b5c")
    String getSku();

    @Schema(description = "Taille.", example = "42")
    String getSize();

    @Schema(description = "Coloris.", example = "Rouge")
    String getColor();

    @Schema(description = "Prix de la variante.")
    MoneyResponseDtoDocumentation getPrice();
}
