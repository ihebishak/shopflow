package com.shopflow.catalog.application.api.command;

import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategorySlug;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Demande de création d'une catégorie par l'admin. Ses annotations sont vérifiées à l'entrée du cas d'usage.
 */
@Getter
@Builder
public class CreateCategoryCommand {

    @NotNull(message = "slug is required")
    private final CategorySlug slug;

    @NotBlank(message = "name is required")
    @Size(max = Category.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
    private final String name;

    /** Facultatif : sans parent, la catégorie est au premier niveau. */
    private final CategorySlug parentSlug;
}
