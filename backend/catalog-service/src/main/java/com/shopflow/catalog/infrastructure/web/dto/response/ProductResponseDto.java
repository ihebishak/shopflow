package com.shopflow.catalog.infrastructure.web.dto.response;

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
public class ProductResponseDto {

    private final String id;
    private final String sellerId;
    private final String name;
    private final String brand;
    private final String category;

    /** {@code null} s'il n'y en a pas. */
    private final String description;

    /** {@code DRAFT}, {@code PUBLISHED} ou {@code UNPUBLISHED}. */
    private final String status;

    private final Instant createdAt;
}
