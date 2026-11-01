package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.ChangeVariantPriceUseCase;
import com.shopflow.catalog.application.api.command.ChangeVariantPriceCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La porte du cas d'usage : créé par Spring, le service vérifie sa commande avant de s'exécuter
 * (créé avec {@code new}, il ne le ferait pas). Ni MongoDB, ni Kafka.
 */
@SpringJUnitConfig(ChangeVariantPriceValidationTest.TestConfiguration.class)
class ChangeVariantPriceValidationTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    @Autowired
    private ChangeVariantPriceUseCase changeVariantPriceUseCase;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductEventPublisher productEventPublisher;

    @Test
    @DisplayName("SHOULD read and save nothing WHEN the command is invalid")
    void shouldReadAndSaveNothingWhenCommandIsInvalid() {
        // GIVEN
        ChangeVariantPriceCommand command = commandPriced(ProductId.newId(), Sku.newSku(), Money.euros(0));

        // WHEN
        Throwable error = catchThrowable(() -> changeVariantPriceUseCase.changeVariantPrice(command));

        // THEN
        assertThat(error).isInstanceOf(ConstraintViolationException.class);
        assertThat(((ConstraintViolationException) error).getConstraintViolations())
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("price must be a positive amount in euros");
        verifyNoInteractions(productRepository, productEventPublisher);
    }

    @Test
    @DisplayName("SHOULD run the use case WHEN the command is valid")
    void shouldRunUseCaseWhenCommandIsValid() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", Money.euros(16_000));
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now())
                .addVariant(variant);
        when(productRepository.findById(airMax.getId())).thenReturn(Optional.of(airMax));

        // WHEN
        changeVariantPriceUseCase.changeVariantPrice(commandPriced(airMax.getId(), variant.getSku(), Money.euros(12_900)));

        // THEN
        verify(productRepository).save(any());
    }

    private static ChangeVariantPriceCommand commandPriced(final ProductId productId, final Sku sku, final Money price) {
        return ChangeVariantPriceCommand.builder().sellerId(KARIM).productId(productId).sku(sku).price(price).build();
    }

    @Configuration
    @Import({ChangeVariantPriceService.class, GetSellerProductService.class})
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {
    }
}
