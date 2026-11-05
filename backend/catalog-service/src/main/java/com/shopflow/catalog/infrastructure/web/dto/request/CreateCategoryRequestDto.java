package com.shopflow.catalog.infrastructure.web.dto.request;

import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.web.swagger.request.CreateCategoryRequestDtoDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Corps JSON de {@code POST /api/v1/admin/categories}.
 * <p>
 * Validé dès l'entrée de l'API ; Swagger affiche ces règles, que le cas d'usage revérifie sur sa commande.
 */
@Getter
@Builder
@Jacksonized
@ToString
@Schema(description = "Demande de création d'une catégorie.")
public class CreateCategoryRequestDto implements CreateCategoryRequestDtoDocumentation {

    @NotBlank(message = "slug is required")
    @Size(max = CategorySlug.MAX_LENGTH, message = "slug must be at most {max} characters")
    private final String slug;

    @NotBlank(message = "name is required")
    @Size(max = Category.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
    private final String name;

    @Size(max = CategorySlug.MAX_LENGTH, message = "parentSlug must be at most {max} characters")
    private final String parentSlug;
}
