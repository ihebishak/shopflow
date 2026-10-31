package com.shopflow.catalog.infrastructure.web.dto.request;

import com.shopflow.catalog.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Demande de création d'un produit (naît en brouillon).")
public class CreateProductRequestDto {

    @Schema(description = "Nom du produit.", example = "Air Max 90 Infrared")
    @NotBlank(message = "name is required")
    @Size(max = Product.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
    private final String name;

    @Schema(description = "Marque.", example = "Nike")
    @NotBlank(message = "brand is required")
    @Size(max = Product.BRAND_MAX_LENGTH, message = "brand must be at most {max} characters")
    private final String brand;

    @Schema(description = "Catégorie (l'arborescence viendra plus tard).", example = "sneakers")
    @NotBlank(message = "category is required")
    @Size(max = Product.CATEGORY_MAX_LENGTH, message = "category must be at most {max} characters")
    private final String category;

    @Schema(description = "Description, facultative.", example = "La réédition du modèle de 1990",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = Product.DESCRIPTION_MAX_LENGTH, message = "description must be at most {max} characters")
    private final String description;
}
