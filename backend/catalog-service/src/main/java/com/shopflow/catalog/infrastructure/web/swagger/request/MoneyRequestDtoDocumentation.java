package com.shopflow.catalog.infrastructure.web.swagger.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation OpenAPI des champs de {@code MoneyRequestDto}, pour que le DTO reste lisible.
 */
public interface MoneyRequestDtoDocumentation {

    @Schema(description = "Montant en centimes, nombre entier (16000 = 160,00 €).", example = "16000")
    Long getAmount();

    @Schema(description = "Devise : seul l'euro est accepté.", allowableValues = {"EUR"}, example = "EUR")
    String getCurrency();
}
