package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande de création d'un produit par un vendeur. Ses annotations sont vérifiées à l'entrée du cas d'usage.
 */
@Getter
@Builder
public class CreateProductCommand {

    @NotNull(message = "sellerId is required")
    private final SellerId sellerId;

    @NotBlank(message = "name is required")
    @Size(max = Product.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
    private final String name;

    @NotBlank(message = "brand is required")
    @Size(max = Product.BRAND_MAX_LENGTH, message = "brand must be at most {max} characters")
    private final String brand;

    @NotBlank(message = "category is required")
    @Size(max = Product.CATEGORY_MAX_LENGTH, message = "category must be at most {max} characters")
    private final String category;

    /** Facultative. */
    @Size(max = Product.DESCRIPTION_MAX_LENGTH, message = "description must be at most {max} characters")
    private final String description;
}
