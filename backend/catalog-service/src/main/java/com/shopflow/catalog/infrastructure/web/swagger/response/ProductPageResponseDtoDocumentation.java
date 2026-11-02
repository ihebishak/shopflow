package com.shopflow.catalog.infrastructure.web.swagger.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation OpenAPI des champs de {@code ProductPageResponseDto}, pour que le DTO reste lisible.
 */
public interface ProductPageResponseDtoDocumentation {

    @Schema(description = "Les produits de la page ; liste vide si la page est au-delà de la fin.")
    List<? extends ProductResponseDtoDocumentation> getContent();

    @Schema(description = "Numéro de la page, à partir de 0.", example = "0")
    int getPage();

    @Schema(description = "Nombre maximum de produits par page.", example = "20")
    int getSize();

    @Schema(description = "Nombre total de produits trouvés, toutes pages confondues.", example = "45")
    long getTotalElements();

    @Schema(description = "Nombre total de pages.", example = "3")
    int getTotalPages();
}
