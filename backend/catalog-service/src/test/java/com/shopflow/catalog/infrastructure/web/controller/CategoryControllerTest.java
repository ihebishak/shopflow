package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.GetCategoryTreeUseCase;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNode;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.web.mapper.CategoryRestMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrôleur seul (couche HTTP) : le cas d'usage est simulé, ni MongoDB ni Kafka.
 */
@WebMvcTest(CategoryController.class)
@Import(CategoryRestMapperImpl.class)
class CategoryControllerTest {

    private static final String CATEGORIES = "/api/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCategoryTreeUseCase getCategoryTreeUseCase;

    @Test
    @DisplayName("SHOULD return 200 with the nested categories WHEN the tree has several levels")
    void shouldReturn200WithNestedCategoriesWhenTreeHasSeveralLevels() throws Exception {
        // GIVEN
        Category shoes = Category.root(new CategorySlug("chaussures"), "Chaussures");
        Category sneakers = shoes.child(new CategorySlug("sneakers"), "Sneakers");
        Category running = sneakers.child(new CategorySlug("running"), "Running");
        when(getCategoryTreeUseCase.getCategoryTree()).thenReturn(CategoryNode.treeOf(List.of(shoes, sneakers, running)));

        // WHEN / THEN
        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("chaussures"))
                .andExpect(jsonPath("$[0].name").value("Chaussures"))
                .andExpect(jsonPath("$[0].children[0].slug").value("sneakers"))
                .andExpect(jsonPath("$[0].children[0].children[0].slug").value("running"))
                .andExpect(jsonPath("$[0].children[0].children[0].name").value("Running"))
                .andExpect(jsonPath("$[0].children[0].children[0].children.length()").value(0));
    }

    @Test
    @DisplayName("SHOULD return 200 with an empty list WHEN there is no category")
    void shouldReturn200WithEmptyListWhenThereIsNoCategory() throws Exception {
        // GIVEN
        when(getCategoryTreeUseCase.getCategoryTree()).thenReturn(List.of());

        // WHEN / THEN
        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
