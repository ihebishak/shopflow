package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.application.api.command.validation.EuroPrice;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.kernel.money.Money;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande de changement du prix d'une variante par un vendeur. Ses annotations sont vérifiées à l'entrée du cas
 * d'usage.
 */
@Getter
@Builder
public class ChangeVariantPriceCommand {

    @NotNull(message = "sellerId is required")
    private final SellerId sellerId;

    @NotNull(message = "productId is required")
    private final ProductId productId;

    @NotNull(message = "sku is required")
    private final Sku sku;

    @NotNull(message = "price is required")
    @EuroPrice
    private final Money price;
}
