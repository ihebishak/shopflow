package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande de publication d'un produit par son vendeur. Ses annotations sont vérifiées à l'entrée du cas d'usage.
 */
@Getter
@Builder
public class PublishProductCommand {

    @NotNull(message = "sellerId is required")
    private final SellerId sellerId;

    @NotNull(message = "productId is required")
    private final ProductId productId;
}
