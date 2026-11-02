package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.ListSellerProductsUseCase;
import com.shopflow.catalog.application.api.query.ListSellerProductsQuery;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.ProductPage;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa demande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(ListSellerProductsValidationTest.TestConfiguration.class)
class ListSellerProductsValidationTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    @Autowired
    private ListSellerProductsUseCase listSellerProductsUseCase;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    @DisplayName("SHOULD read nothing WHEN the query is invalid")
    void shouldReadNothingWhenQueryIsInvalid() {
        // GIVEN
        ListSellerProductsQuery query = ListSellerProductsQuery.builder().sellerId(KARIM).page(0).size(500).build();

        // WHEN
        Throwable error = catchThrowable(() -> listSellerProductsUseCase.listSellerProducts(query));

        // THEN
        assertThat(error).isInstanceOf(ConstraintViolationException.class);
        assertThat(((ConstraintViolationException) error).getConstraintViolations())
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("size must be between 1 and 100");
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("SHOULD run the use case WHEN the query is valid")
    void shouldRunUseCaseWhenQueryIsValid() {
        // GIVEN
        ProductPage page = ProductPage.builder().content(List.of()).page(0).size(20).totalElements(0).build();
        when(productRepository.findBySeller(KARIM, null, 0, 20)).thenReturn(page);
        ListSellerProductsQuery query = ListSellerProductsQuery.builder().sellerId(KARIM).page(0).size(20).build();

        // WHEN
        ProductPage listed = listSellerProductsUseCase.listSellerProducts(query);

        // THEN
        assertThat(listed).isSameAs(page);
    }

    @Configuration
    @Import(ListSellerProductsService.class)
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {
    }
}
