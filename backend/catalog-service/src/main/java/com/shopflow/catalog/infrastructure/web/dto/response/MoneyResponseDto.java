package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.MoneyResponseDtoDocumentation;
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
public class MoneyResponseDto implements MoneyResponseDtoDocumentation {

    private final long amount;

    private final String currency;
}
