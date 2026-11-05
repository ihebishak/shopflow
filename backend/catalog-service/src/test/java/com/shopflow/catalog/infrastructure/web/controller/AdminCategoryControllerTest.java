package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.CreateCategoryUseCase;
import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryAlreadyExistsException;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.CategoryTooDeepException;
import com.shopflow.catalog.infrastructure.web.mapper.CategoryRestMapperImpl;
import com.shopflow.kernel.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrôleur seul (couche HTTP) : le cas d'usage est simulé, ni MongoDB ni Kafka.
 * Le gestionnaire d'erreurs du starter est actif : les réponses d'erreur sont celles du vrai service.
 */
@WebMvcTest(AdminCategoryController.class)
@Import(CategoryRestMapperImpl.class)
class AdminCategoryControllerTest {

    private static final String CATEGORIES = "/api/v1/admin/categories";
    private static final CategorySlug SHOES = new CategorySlug("chaussures");
    private static final CategorySlug SNEAKERS = new CategorySlug("sneakers");
    private static final String SNEAKERS_JSON = """
            { "slug": "sneakers", "name": "Sneakers", "parentSlug": "chaussures" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @Test
    @DisplayName("SHOULD return 201 with the category WHEN it has a parent")
    void shouldReturn201WhenCategoryHasParent() throws Exception {
        // GIVEN
        when(createCategoryUseCase.createCategory(any()))
                .thenReturn(Category.root(SHOES, "Chaussures").child(SNEAKERS, "Sneakers"));

        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content(SNEAKERS_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slug").value("sneakers"))
                .andExpect(jsonPath("$.name").value("Sneakers"))
                .andExpect(jsonPath("$.ancestors.length()").value(1))
                .andExpect(jsonPath("$.ancestors[0]").value("chaussures"));
        ArgumentCaptor<CreateCategoryCommand> command = ArgumentCaptor.forClass(CreateCategoryCommand.class);
        verify(createCategoryUseCase).createCategory(command.capture());
        assertThat(command.getValue().getSlug()).isEqualTo(SNEAKERS);
        assertThat(command.getValue().getName()).isEqualTo("Sneakers");
        assertThat(command.getValue().getParentSlug()).isEqualTo(SHOES);
    }

    @Test
    @DisplayName("SHOULD return 201 with no ancestor WHEN there is no parent")
    void shouldReturn201WithNoAncestorWhenThereIsNoParent() throws Exception {
        // GIVEN
        when(createCategoryUseCase.createCategory(any())).thenReturn(Category.root(SHOES, "Chaussures"));

        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "slug": "chaussures", "name": "Chaussures" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ancestors.length()").value(0));
        ArgumentCaptor<CreateCategoryCommand> command = ArgumentCaptor.forClass(CreateCategoryCommand.class);
        verify(createCategoryUseCase).createCategory(command.capture());
        assertThat(command.getValue().getParentSlug()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field, without calling the use case, WHEN a field is invalid")
    void shouldReturn400WithFieldWhenFieldIsInvalid(final String json, final String field) throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verify(createCategoryUseCase, never()).createCategory(any());
    }

    static Stream<Arguments> invalidRequests() {
        String tooLong = "a".repeat(61);
        return Stream.of(
                arguments("{ \"name\": \"Sneakers\" }", "slug"),
                arguments("{ \"slug\": \" \", \"name\": \"Sneakers\" }", "slug"),
                arguments("{ \"slug\": \"" + tooLong + "\", \"name\": \"Sneakers\" }", "slug"),
                arguments("{ \"slug\": \"sneakers\" }", "name"),
                arguments("{ \"slug\": \"sneakers\", \"name\": \" \" }", "name"),
                arguments("{ \"slug\": \"sneakers\", \"name\": \"" + tooLong + "\" }", "name"),
                arguments("{ \"slug\": \"sneakers\", \"name\": \"Sneakers\", \"parentSlug\": \"" + tooLong + "\" }",
                        "parentSlug"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{ \"slug\": \"Sneakers\", \"name\": \"Sneakers\" }",
            "{ \"slug\": \"sneakers\", \"name\": \"Sneakers\", \"parentSlug\": \"Chaussures\" }",
            "{ \"slug\": \"sneakers\", \"name\": \"Sneakers\", \"parentSlug\": \"\" }"
    })
    @DisplayName("SHOULD return 400 INVALID_CATEGORY_SLUG, without calling the use case, WHEN a slug is badly formed")
    void shouldReturn400WhenSlugIsBadlyFormed(final String json) throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_SLUG"));
        verify(createCategoryUseCase, never()).createCategory(any());
    }

    @Test
    @DisplayName("SHOULD return 400 MALFORMED_REQUEST, without calling the use case, WHEN the body is not valid JSON")
    void shouldReturn400WhenBodyIsNotValidJson() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content("{ \"slug\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verify(createCategoryUseCase, never()).createCategory(any());
    }

    @ParameterizedTest
    @MethodSource("businessErrors")
    @DisplayName("SHOULD return the status and the code of the error WHEN the use case refuses the category")
    void shouldReturnStatusAndCodeWhenUseCaseRefusesCategory(final BusinessException exception, final HttpStatus httpStatus)
            throws Exception {
        // GIVEN
        when(createCategoryUseCase.createCategory(any())).thenThrow(exception);

        // WHEN / THEN
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content(SNEAKERS_JSON))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(exception.getCode()));
    }

    static Stream<Arguments> businessErrors() {
        return Stream.of(
                arguments(new CategoryNotFoundException(SHOES), HttpStatus.NOT_FOUND),
                arguments(new CategoryTooDeepException(SHOES), HttpStatus.CONFLICT),
                arguments(new CategoryAlreadyExistsException(SNEAKERS), HttpStatus.CONFLICT));
    }
}
