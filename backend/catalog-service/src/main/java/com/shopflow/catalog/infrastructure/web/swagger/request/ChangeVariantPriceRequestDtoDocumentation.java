package com.shopflow.catalog.infrastructure.web.swagger.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code ChangeVariantPriceRequestDto}, pour que le DTO reste lisible.
 */
public interface ChangeVariantPriceRequestDtoDocumentation {

    @Schema(description = "Nouveau prix de la variante.")
    MoneyRequestDtoDocumentation getPrice();
}
