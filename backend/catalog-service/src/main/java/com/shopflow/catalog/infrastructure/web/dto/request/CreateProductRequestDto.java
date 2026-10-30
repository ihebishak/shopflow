package com.shopflow.catalog.infrastructure.web.dto.request;

import com.shopflow.catalog.domain.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Corps JSON de {@code POST /api/v1/seller/products}.
 * <p>
 * Validé dès l'entrée de l'API ; les limites reprennent celles du domaine, qui garde ses propres vérifications.
 */
@Getter
@Builder
@Jacksonized
@ToString
public class CreateProductRequestDto {

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
