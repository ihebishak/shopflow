package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.application.api.command.validation.EuroPrice;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.kernel.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande d'ajout d'une variante par un vendeur. Ses annotations sont vérifiées à l'entrée du cas d'usage.
 */
@Getter
@Builder
public class AddVariantCommand {

    @NotNull(message = "sellerId is required")
    private final SellerId sellerId;

    @NotNull(message = "productId is required")
    private final ProductId productId;

    @NotBlank(message = "size is required")
    @Size(max = Variant.SIZE_MAX_LENGTH, message = "size must be at most {max} characters")
    private final String size;

    @NotBlank(message = "color is required")
    @Size(max = Variant.COLOR_MAX_LENGTH, message = "color must be at most {max} characters")
    private final String color;

    @NotNull(message = "price is required")
    @EuroPrice
    private final Money price;
}
