package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.CategoryResponseDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Catégorie renvoyée par l'API.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Une catégorie de l'arbre du catalogue.")
public class CategoryResponseDto implements CategoryResponseDtoDocumentation {

    private final String slug;

    private final String name;

    private final List<String> ancestors;
}
