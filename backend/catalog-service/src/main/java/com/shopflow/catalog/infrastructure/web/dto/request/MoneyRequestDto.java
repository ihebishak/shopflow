package com.shopflow.catalog.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Montant reçu par l'API, en unités mineures. La devise est vérifiée ici : un code inconnu ne peut pas devenir un
 * {@link com.shopflow.kernel.money.Money}.
 */
@Getter
@Builder
@Jacksonized
@ToString
@Schema(description = "Un montant strictement positif, en euros.")
public class MoneyRequestDto {

    @Schema(description = "Montant en centimes, nombre entier (16000 = 160,00 €).", example = "16000")
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private final Long amount;

    @Schema(description = "Devise : seul l'euro est accepté.", allowableValues = {"EUR"}, example = "EUR")
    @NotNull(message = "currency is required")
    @Pattern(regexp = "EUR", message = "currency must be EUR")
    private final String currency;
}
