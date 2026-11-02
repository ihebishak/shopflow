package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.ErrorKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CategorySlugTest {

    @ParameterizedTest
    @ValueSource(strings = {"sneakers", "hoodies-sweats", "t-shirts", "taille-42", "3d"})
    @DisplayName("SHOULD keep the slug WHEN its format is valid")
    void shouldKeepSlugWhenFormatIsValid(final String value) {
        // WHEN
        CategorySlug slug = new CategorySlug(value);

        // THEN
        assertThat(slug.getValue()).isEqualTo(value);
        assertThat(slug).hasToString(value);
    }

    @Test
    @DisplayName("SHOULD accept the slug WHEN it has exactly the maximum length")
    void shouldAcceptSlugWhenItHasMaximumLength() {
        // GIVEN
        String longest = "a".repeat(CategorySlug.MAX_LENGTH);

        // WHEN
        CategorySlug slug = new CategorySlug(longest);

        // THEN
        assertThat(slug.getValue()).isEqualTo(longest);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Sneakers", "t shirts", "-running", "running-", "baskets--cuir",
            "chaussures-été", "hoodies_sweats"})
    @DisplayName("SHOULD reject the slug WHEN its format is invalid")
    void shouldRejectSlugWhenFormatIsInvalid(final String value) {
        // WHEN
        Throwable thrown = catchThrowable(() -> new CategorySlug(value));

        // THEN
        assertThat(thrown).isInstanceOf(InvalidCategorySlugException.class)
                .hasFieldOrPropertyWithValue("kind", ErrorKind.INVALID)
                .hasFieldOrPropertyWithValue("code", InvalidCategorySlugException.CODE);
    }

    @Test
    @DisplayName("SHOULD reject the slug WHEN it is one character too long")
    void shouldRejectSlugWhenItIsTooLong() {
        // GIVEN
        String tooLong = "a".repeat(CategorySlug.MAX_LENGTH + 1);

        // WHEN
        Throwable thrown = catchThrowable(() -> new CategorySlug(tooLong));

        // THEN
        assertThat(thrown).isInstanceOf(InvalidCategorySlugException.class);
    }
}
