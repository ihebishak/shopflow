package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.SellerId;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande de création d'un produit par un vendeur. Les règles sont vérifiées par le domaine.
 */
@Getter
@Builder
public class CreateProductCommand {

    private final SellerId sellerId;
    private final String name;
    private final String brand;
    private final String category;

    /** Facultative. */
    private final String description;
}
