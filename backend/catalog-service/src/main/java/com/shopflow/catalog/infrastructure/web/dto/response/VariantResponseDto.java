package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.VariantResponseDtoDocumentation;
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
public class VariantResponseDto implements VariantResponseDtoDocumentation {

    private final String sku;

    private final String size;

    private final String color;

    private final MoneyResponseDto price;
}
