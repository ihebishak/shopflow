package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.ProductPageResponseDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Une page de produits renvoyée par l'API vendeur.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Une page des produits du vendeur, les plus récents d'abord.")
public class ProductPageResponseDto implements ProductPageResponseDtoDocumentation {

    private final List<ProductResponseDto> content;

    private final int page;

    private final int size;

    private final long totalElements;

    private final int totalPages;
}
