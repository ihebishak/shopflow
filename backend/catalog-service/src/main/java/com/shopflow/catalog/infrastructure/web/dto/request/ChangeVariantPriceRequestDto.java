package com.shopflow.catalog.infrastructure.web.dto.request;

import com.shopflow.catalog.infrastructure.web.swagger.request.ChangeVariantPriceRequestDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Corps JSON de {@code PUT /api/v1/seller/products/{productId}/variants/{sku}/price}.
 * <p>
 * Validé dès l'entrée de l'API ; Swagger affiche ces règles, que le cas d'usage revérifie sur sa commande.
 */
@Getter
@Builder
@Jacksonized
@ToString
@Schema(description = "Demande de changement du prix d'une variante.")
public class ChangeVariantPriceRequestDto implements ChangeVariantPriceRequestDtoDocumentation {

    @NotNull(message = "price is required")
    @Valid
    private final MoneyRequestDto price;
}
