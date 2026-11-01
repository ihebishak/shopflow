package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.UpdateProductUseCase;
import com.shopflow.catalog.application.api.command.UpdateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa commande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(UpdateProductValidationTest.TestConfiguration.class)
class UpdateProductValidationTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    @Autowired
    private UpdateProductUseCase updateProductUseCase;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductEventPublisher productEventPublisher;

    @Test
    @DisplayName("SHOULD read and save nothing WHEN the command is invalid")
    void shouldReadAndSaveNothingWhenCommandIsInvalid() {
        // GIVEN
        UpdateProductCommand command = commandNamed(ProductId.newId(), " ");

        // WHEN
        Throwable error = catchThrowable(() -> updateProductUseCase.updateProduct(command));

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
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
        when(productRepository.findById(airMax.getId())).thenReturn(Optional.of(airMax));

        // WHEN
        updateProductUseCase.updateProduct(commandNamed(airMax.getId(), "Stan Smith"));

        // THEN
        verify(productRepository).save(any());
    }

    private static UpdateProductCommand commandNamed(final ProductId productId, final String name) {
        return UpdateProductCommand.builder().sellerId(KARIM).productId(productId).name(name).brand("Nike")
                .category("sneakers").build();
    }

    @Configuration
    @Import({UpdateProductService.class, GetSellerProductService.class})
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {
    }
}
