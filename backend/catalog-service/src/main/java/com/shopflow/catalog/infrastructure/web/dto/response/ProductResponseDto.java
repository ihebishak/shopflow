package com.shopflow.catalog.infrastructure.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Produit renvoyé par l'API vendeur. Séparé du domaine : le format de l'API reste stable si le domaine évolue.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Un produit du vendeur.")
public class ProductResponseDto {

    @Schema(description = "Identifiant du produit (UUID v7).", example = "01a19c2e-7b4f-7d10-9c3a-3e5f1b2c4d6e")
    private final String id;

    @Schema(description = "Identifiant du vendeur propriétaire.", example = "seller-karim")
    private final String sellerId;

    @Schema(example = "Air Max 90 Infrared")
    private final String name;

    @Schema(example = "Nike")
    private final String brand;

    @Schema(example = "sneakers")
    private final String category;

    @Schema(description = "null s'il n'y en a pas.", example = "La réédition du modèle de 1990")
    private final String description;

    @Schema(description = "Cycle de vie du produit.", allowableValues = {"DRAFT", "PUBLISHED", "UNPUBLISHED"},
            example = "DRAFT")
    private final String status;

    @Schema(description = "Date de création (UTC).", example = "2026-10-31T10:00:00.123Z")
    private final Instant createdAt;
}
