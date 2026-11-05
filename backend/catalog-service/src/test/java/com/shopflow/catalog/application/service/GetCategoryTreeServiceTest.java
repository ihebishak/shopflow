package com.shopflow.catalog.application.service;

import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNode;
import com.shopflow.catalog.domain.CategorySlug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetCategoryTreeServiceTest {

    private final InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
    private final GetCategoryTreeService getCategoryTreeService = new GetCategoryTreeService(categoryRepository);

    @Test
    @DisplayName("SHOULD return an empty tree WHEN no category has been created")
    void shouldReturnEmptyTreeWhenNoCategoryHasBeenCreated() {
        // WHEN
        List<CategoryNode> tree = getCategoryTreeService.getCategoryTree();

        // THEN
        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("SHOULD return the saved categories as a tree WHEN some have been created")
    void shouldReturnSavedCategoriesAsTreeWhenSomeHaveBeenCreated() {
        // GIVEN
        Category shoes = Category.root(new CategorySlug("chaussures"), "Chaussures");
        categoryRepository.create(shoes);
        categoryRepository.create(shoes.child(new CategorySlug("sneakers"), "Sneakers"));

        // WHEN
        List<CategoryNode> tree = getCategoryTreeService.getCategoryTree();

        // THEN
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("Chaussures");
        assertThat(tree.get(0).getChildren()).extracting(CategoryNode::getName).containsExactly("Sneakers");
    }
}
