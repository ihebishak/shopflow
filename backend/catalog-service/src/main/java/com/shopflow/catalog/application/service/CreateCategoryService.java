package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateCategoryUseCase;
import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Crée une catégorie de premier niveau, ou la fait créer par son parent, qui lui transmet ses ancêtres.
 * <p>
 * Sans transaction : il n'y a qu'une écriture, et aucun événement à enregistrer avec elle.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateCategoryService implements CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(final CreateCategoryCommand command) {
        Category category = command.getParentSlug() == null
                ? Category.root(command.getSlug(), command.getName())
                : findParent(command.getParentSlug()).child(command.getSlug(), command.getName());
        categoryRepository.create(category);
        return category;
    }

    private Category findParent(final CategorySlug parentSlug) {
        return categoryRepository.findBySlug(parentSlug).orElseThrow(() -> new CategoryNotFoundException(parentSlug));
    }
}
