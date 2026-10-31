package com.shopflow.catalog.infrastructure.web.mapper;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.MoneyResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le code généré par MapStruct, testé sans Spring.
 */
class ProductRestMapperTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    private final ProductRestMapper productRestMapper = new ProductRestMapperImpl();

    @Test
    @DisplayName("SHOULD build the command with the seller WHEN mapping a creation request")
    void shouldBuildCommandWhenMappingCreationRequest() {
        // GIVEN
        CreateProductRequestDto requestDto = CreateProductRequestDto.builder()
                .name("Air Max 90 Infrared").brand("Nike").category("sneakers").description("Réédition 1990").build();

        // WHEN
        CreateProductCommand command = productRestMapper.toCommand(requestDto, KARIM);

        // THEN
        assertThat(command.getSellerId()).isEqualTo(KARIM);
        assertThat(command.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(command.getBrand()).isEqualTo("Nike");
        assertThat(command.getCategory()).isEqualTo("sneakers");
        assertThat(command.getDescription()).isEqualTo("Réédition 1990");
    }

    @Test
    @DisplayName("SHOULD copy every field WHEN mapping a product to the response")
    void shouldCopyEveryFieldWhenMappingProductToResponse() {
        // GIVEN
        Instant now = Instant.parse("2026-10-31T10:00:00Z");
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", "Réédition 1990", now);

        // WHEN
        ProductResponseDto responseDto = productRestMapper.toResponseDto(airMax);

        // THEN
        assertThat(responseDto.getId()).isEqualTo(airMax.getId().toString());
        assertThat(responseDto.getSellerId()).isEqualTo("seller-karim");
        assertThat(responseDto.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(responseDto.getBrand()).isEqualTo("Nike");
        assertThat(responseDto.getCategory()).isEqualTo("sneakers");
        assertThat(responseDto.getDescription()).isEqualTo("Réédition 1990");
        assertThat(responseDto.getStatus()).isEqualTo("DRAFT");
        assertThat(responseDto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("SHOULD return a null description WHEN the product has none")
    void shouldReturnNullDescriptionWhenProductHasNone() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());

        // WHEN
        ProductResponseDto responseDto = productRestMapper.toResponseDto(airMax);

        // THEN
        assertThat(responseDto.getDescription()).isNull();
    }

    @Test
    @DisplayName("SHOULD copy every variant field WHEN the product has a variant")
    void shouldCopyEveryVariantFieldWhenProductHasVariant() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
        Variant variant = airMax.addVariant("42", "Rouge", Money.euros(16000));

        // WHEN
        ProductResponseDto responseDto = productRestMapper.toResponseDto(airMax);

        // THEN
        assertThat(responseDto.getVariants()).containsExactly(VariantResponseDto.builder()
                .sku(variant.getSku().toString())
                .size("42")
                .color("Rouge")
                .price(MoneyResponseDto.builder().amount(16000).currency("EUR").build())
                .build());
    }

    @Test
    @DisplayName("SHOULD return an empty list WHEN the product has no variant")
    void shouldReturnEmptyListWhenProductHasNoVariant() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());

        // WHEN
        ProductResponseDto responseDto = productRestMapper.toResponseDto(airMax);

        // THEN
        assertThat(responseDto.getVariants()).isNotNull().isEmpty();
    }
}
