package com.shopflow.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le calcul du nombre de pages.
 */
class ProductPageTest {

    @ParameterizedTest
    @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "40, 20, 2", "45, 20, 3"})
    @DisplayName("SHOULD count the pages, the last one included WHEN it is not full")
    void shouldCountPages(final long totalElements, final int size, final int totalPages) {
        // GIVEN
        ProductPage page = ProductPage.builder().content(List.of()).page(0).size(size).totalElements(totalElements)
                .build();

        // WHEN / THEN
        assertThat(page.getTotalPages()).isEqualTo(totalPages);
    }
}
