package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.AddVariantUseCase;
import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.kernel.money.Money;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa commande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(AddVariantValidationTest.TestConfiguration.class)
class AddVariantValidationTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    @Autowired
    private AddVariantUseCase addVariantUseCase;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    @DisplayName("SHOULD read and save nothing WHEN the command is invalid")
    void shouldReadAndSaveNothingWhenCommandIsInvalid() {
        // GIVEN
        AddVariantCommand command = commandPriced(ProductId.newId(), Money.euros(0));

        // WHEN
        Throwable error = catchThrowable(() -> addVariantUseCase.addVariant(command));

        // THEN
        assertThat(error).isInstanceOf(ConstraintViolationException.class);
        assertThat(((ConstraintViolationException) error).getConstraintViolations())
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("price must be a positive amount in euros");
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("SHOULD run the use case WHEN the command is valid")
    void shouldRunUseCaseWhenCommandIsValid() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
        when(productRepository.findById(airMax.getId())).thenReturn(Optional.of(airMax));

        // WHEN
        addVariantUseCase.addVariant(commandPriced(airMax.getId(), Money.euros(16_000)));

        // THEN
        verify(productRepository).save(airMax);
    }

    private static AddVariantCommand commandPriced(final ProductId productId, final Money price) {
        return AddVariantCommand.builder().sellerId(KARIM).productId(productId).size("42").color("Rouge").price(price)
                .build();
    }

    @Configuration
    @Import({AddVariantService.class, GetSellerProductService.class})
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {
    }
}
