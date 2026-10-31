package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.SellerId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa commande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(CreateProductValidationTest.TestConfiguration.class)
class CreateProductValidationTest {

    @Autowired
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductEventPublisher productEventPublisher;

    @Test
    @DisplayName("SHOULD save and announce nothing WHEN the command is invalid")
    void shouldSaveAndAnnounceNothingWhenCommandIsInvalid() {
        // GIVEN
        CreateProductCommand command = airMaxCommand(" ");

        // WHEN
        Throwable error = catchThrowable(() -> createProductUseCase.createProduct(command));

        // THEN
        assertThat(error).isInstanceOf(ConstraintViolationException.class);
        assertThat(((ConstraintViolationException) error).getConstraintViolations())
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("name is required");
        verifyNoInteractions(productRepository, productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD run the use case WHEN the command is valid")
    void shouldRunUseCaseWhenCommandIsValid() {
        // GIVEN
        CreateProductCommand command = airMaxCommand("Air Max 90 Infrared");

        // WHEN
        Product product = createProductUseCase.createProduct(command);

        // THEN
        verify(productRepository).save(product);
        verify(productEventPublisher).publishProductCreated(product);
    }

    private static CreateProductCommand airMaxCommand(final String name) {
        return CreateProductCommand.builder().sellerId(new SellerId("seller-karim")).name(name).brand("Nike")
                .category("sneakers").build();
    }

    @Configuration
    @Import(CreateProductService.class)
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
