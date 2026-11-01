package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Documentation OpenAPI des champs de {@code ProductResponseDto}, pour que le DTO reste lisible.
 */
public interface ProductResponseDtoDocumentation {

    @Schema(description = "Identifiant du produit (UUID v7).", example = "01a19c2e-7b4f-7d10-9c3a-3e5f1b2c4d6e")
    String getId();

    @Schema(description = "Identifiant du vendeur propriétaire.", example = "seller-karim")
    String getSellerId();

    @Schema(example = "Air Max 90 Infrared")
    String getName();

    @Schema(example = "Nike")
    String getBrand();

    @Schema(example = "sneakers")
    String getCategory();

    @Schema(description = "null s'il n'y en a pas.", example = "La réédition du modèle de 1990")
    String getDescription();

    @Schema(description = "Cycle de vie du produit.", allowableValues = {"DRAFT", "PUBLISHED", "UNPUBLISHED"},
            example = "DRAFT")
    String getStatus();

    @Schema(description = "Date de création (UTC).", example = "2026-10-31T10:00:00.123Z")
    Instant getCreatedAt();

    @Schema(description = "Variantes du produit ; liste vide s'il n'y en a pas.")
    List<? extends VariantResponseDtoDocumentation> getVariants();
}
