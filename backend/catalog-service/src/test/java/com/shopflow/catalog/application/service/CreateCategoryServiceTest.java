package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryAlreadyExistsException;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.CategoryTooDeepException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CreateCategoryServiceTest {

    private static final CategorySlug SHOES = new CategorySlug("chaussures");
    private static final CategorySlug SNEAKERS = new CategorySlug("sneakers");
    private static final CategorySlug RUNNING = new CategorySlug("running");

    private final InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private final CreateCategoryService createCategoryService = new CreateCategoryService(categoryRepository);

    @Test
    @DisplayName("SHOULD create a first-level category WHEN there is no parent")
    void shouldCreateFirstLevelCategoryWhenThereIsNoParent() {
        // WHEN
        Category shoes = createCategoryService.createCategory(command(SHOES, "Chaussures", null));

        // THEN
        assertThat(shoes.getSlug()).isEqualTo(SHOES);
        assertThat(shoes.getName()).isEqualTo("Chaussures");
        assertThat(shoes.getAncestors()).isEmpty();
        assertThat(categoryRepository.findBySlug(SHOES)).containsSame(shoes);
    }

    @Test
    @DisplayName("SHOULD pass the ancestors of the parent down WHEN the category has a parent")
    void shouldPassAncestorsDownWhenCategoryHasParent() {
        // GIVEN
        createCategoryService.createCategory(command(SHOES, "Chaussures", null));
        createCategoryService.createCategory(command(SNEAKERS, "Sneakers", SHOES));

        // WHEN
        Category running = createCategoryService.createCategory(command(RUNNING, "Running", SNEAKERS));

        // THEN
        assertThat(running.getAncestors()).containsExactly(SHOES, SNEAKERS);
        assertThat(categoryRepository.findBySlug(RUNNING)).containsSame(running);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the parent does not exist")
    void shouldSaveNothingWhenParentDoesNotExist() {
        // WHEN
        Throwable thrown = catchThrowable(
                () -> createCategoryService.createCategory(command(SNEAKERS, "Sneakers", SHOES)));

        // THEN
        assertThat(thrown).isInstanceOf(CategoryNotFoundException.class).hasMessageContaining("chaussures");
        assertThat(categoryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the parent is at the last level")
    void shouldSaveNothingWhenParentIsAtLastLevel() {
        // GIVEN
        createCategoryService.createCategory(command(SHOES, "Chaussures", null));
        createCategoryService.createCategory(command(SNEAKERS, "Sneakers", SHOES));
        createCategoryService.createCategory(command(RUNNING, "Running", SNEAKERS));
        CategorySlug trail = new CategorySlug("trail");

        // WHEN
        Throwable thrown = catchThrowable(
                () -> createCategoryService.createCategory(command(trail, "Trail", RUNNING)));

        // THEN
        assertThat(thrown).isInstanceOf(CategoryTooDeepException.class);
        assertThat(categoryRepository.findBySlug(trail)).isEmpty();
    }

    @Test
    @DisplayName("SHOULD keep the original WHEN the slug is already taken")
    void shouldKeepOriginalWhenSlugIsTaken() {
        // GIVEN
        Category original = createCategoryService.createCategory(command(SHOES, "Chaussures", null));

        // WHEN
        Throwable thrown = catchThrowable(
                () -> createCategoryService.createCategory(command(SHOES, "Souliers", null)));

        // THEN
        assertThat(thrown).isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(categoryRepository.findBySlug(SHOES)).containsSame(original);
    }

    private static CreateCategoryCommand command(final CategorySlug slug, final String name,
                                                 final CategorySlug parentSlug) {
        return CreateCategoryCommand.builder().slug(slug).name(name).parentSlug(parentSlug).build();
    }
}
