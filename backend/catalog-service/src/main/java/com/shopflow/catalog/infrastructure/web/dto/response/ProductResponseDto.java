package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.ProductResponseDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Produit renvoyé par l'API vendeur. Séparé du domaine : le format de l'API reste stable si le domaine évolue.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Un produit du vendeur.")
public class ProductResponseDto implements ProductResponseDtoDocumentation {

    private final String id;

    private final String sellerId;

    private final String name;

    private final String brand;

    private final String category;

    private final String description;

    private final String status;

    private final Instant createdAt;

    private final List<VariantResponseDto> variants;
}
