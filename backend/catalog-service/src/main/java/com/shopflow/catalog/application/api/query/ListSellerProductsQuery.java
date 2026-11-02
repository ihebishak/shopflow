package com.shopflow.catalog.application.api.query;

import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande d'une page des produits d'un vendeur. Ses annotations sont vérifiées à l'entrée du cas d'usage.
 */
@Getter
@Builder
public class ListSellerProductsQuery {

    public static final int MAX_SIZE = 100;

    @NotNull(message = "sellerId is required")
    private final SellerId sellerId;

    /** Le statut recherché, {@code null} pour tous les statuts. */
    private final ProductStatus status;

    @Min(value = 0, message = "page must not be negative")
    private final int page;

    @Min(value = 1, message = "size must be between 1 and 100")
    @Max(value = MAX_SIZE, message = "size must be between 1 and 100")
    private final int size;
}
