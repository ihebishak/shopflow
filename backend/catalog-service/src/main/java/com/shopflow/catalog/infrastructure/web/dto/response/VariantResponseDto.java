package com.shopflow.catalog.infrastructure.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Variante renvoyée par l'API vendeur.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Une variante d'un produit.")
public class VariantResponseDto {

    @Schema(description = "Identifiant de la variante (UUID v7), généré par le serveur.",
            example = "01a19c2f-3c1d-7e20-8b4a-9d6e2f1a3b5c")
    private final String sku;

    @Schema(description = "Taille.", example = "42")
    private final String size;

    @Schema(description = "Coloris.", example = "Rouge")
    private final String color;

    @Schema(description = "Prix de la variante.")
    private final MoneyResponseDto price;
}
