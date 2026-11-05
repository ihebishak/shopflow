package com.shopflow.catalog.infrastructure.web.dto.response;

import com.shopflow.catalog.infrastructure.web.swagger.response.CategoryNodeResponseDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Une catégorie de l'arbre renvoyé par l'API, avec ses sous-catégories.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
@Schema(description = "Une catégorie de l'arbre, avec ses sous-catégories.")
public class CategoryNodeResponseDto implements CategoryNodeResponseDtoDocumentation {

    private final String slug;

    private final String name;

    private final List<CategoryNodeResponseDto> children;
}
