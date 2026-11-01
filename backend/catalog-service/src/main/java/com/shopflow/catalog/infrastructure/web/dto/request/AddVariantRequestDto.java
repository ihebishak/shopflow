package com.shopflow.catalog.infrastructure.web.dto.request;

import com.shopflow.catalog.domain.Variant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Corps JSON de {@code POST /api/v1/seller/products/{productId}/variants}.
 * <p>
 * Validé dès l'entrée de l'API ; Swagger affiche ces règles, que le cas d'usage revérifie sur sa commande.
 */
@Getter
@Builder
@Jacksonized
@ToString
@Schema(description = "Demande d'ajout d'une variante à un produit.")
public class AddVariantRequestDto {

    @Schema(description = "Taille, en texte libre.", example = "42")
    @NotBlank(message = "size is required")
    @Size(max = Variant.SIZE_MAX_LENGTH, message = "size must be at most {max} characters")
    private final String size;

    @Schema(description = "Coloris, en texte libre.", example = "Rouge")
    @NotBlank(message = "color is required")
    @Size(max = Variant.COLOR_MAX_LENGTH, message = "color must be at most {max} characters")
    private final String color;

    @Schema(description = "Prix de la variante.")
    @NotNull(message = "price is required")
    @Valid
    private final MoneyRequestDto price;
}
