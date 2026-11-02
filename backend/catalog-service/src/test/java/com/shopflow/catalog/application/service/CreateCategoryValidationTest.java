package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateCategoryUseCase;
import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategorySlug;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa commande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(CreateCategoryValidationTest.TestConfiguration.class)
class CreateCategoryValidationTest {

    private static final CategorySlug SHOES = new CategorySlug("chaussures");

    @Autowired
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @ParameterizedTest
    @MethodSource("invalidCommands")
    @DisplayName("SHOULD read and save nothing WHEN the command is invalid")
    void shouldReadAndSaveNothingWhenCommandIsInvalid(final CreateCategoryCommand command, final String message) {
        // WHEN
        Throwable error = catchThrowable(() -> createCategoryUseCase.createCategory(command));

        // THEN
        assertThat(error).isInstanceOf(ConstraintViolationException.class);
        assertThat(((ConstraintViolationException) error).getConstraintViolations())
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(message);
        verifyNoInteractions(categoryRepository);
    }

    static Stream<Arguments> invalidCommands() {
        return Stream.of(
                arguments(CreateCategoryCommand.builder().name("Chaussures").build(), "slug is required"),
                arguments(CreateCategoryCommand.builder().slug(SHOES).name(" ").build(), "name is required"),
                arguments(CreateCategoryCommand.builder().slug(SHOES).name("a".repeat(Category.NAME_MAX_LENGTH + 1))
                        .build(), "name must be at most 60 characters"));
    }

    @Test
    @DisplayName("SHOULD run the use case WHEN the command is valid")
    void shouldRunUseCaseWhenCommandIsValid() {
        // GIVEN
        CreateCategoryCommand command = CreateCategoryCommand.builder().slug(SHOES)
                .name("a".repeat(Category.NAME_MAX_LENGTH)).build();

        // WHEN
        Category category = createCategoryUseCase.createCategory(command);

        // THEN
        verify(categoryRepository).create(category);
    }

    @Configuration
    @Import(CreateCategoryService.class)
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {
    }
}
