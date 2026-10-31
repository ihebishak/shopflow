package com.shopflow.catalog.infrastructure.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Montant renvoyé par l'API, en unités mineures : jamais de nombre à virgule pour de l'argent.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Un montant.")
public class MoneyResponseDto {

    @Schema(description = "Montant en centimes (16000 = 160,00 €).", example = "16000")
    private final long amount;

    @Schema(description = "Devise (code ISO 4217).", example = "EUR")
    private final String currency;
}
