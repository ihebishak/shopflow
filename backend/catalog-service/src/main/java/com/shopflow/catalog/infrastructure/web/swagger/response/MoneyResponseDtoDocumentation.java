package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code MoneyResponseDto}, pour que le DTO reste lisible.
 */
public interface MoneyResponseDtoDocumentation {

    @Schema(description = "Montant en centimes (16000 = 160,00 €).", example = "16000")
    long getAmount();

    @Schema(description = "Devise (code ISO 4217).", example = "EUR")
    String getCurrency();
}
