package com.shopflow.catalog.infrastructure.web.mapper;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;

/**
 * Conversions entre les DTO de l'API et la couche application. Le code est généré par MapStruct à la compilation
 * ({@code ProductRestMapperImpl}) ; les méthodes {@code default} lui expliquent les types du domaine.
 */
@Mapper
public interface ProductRestMapper {

    @Mapping(target = "sellerId", source = "sellerId")
    CreateProductCommand toCommand(CreateProductRequestDto requestDto, SellerId sellerId);

    ProductResponseDto toResponseDto(Product product);

    VariantResponseDto toResponseDto(Variant variant);

    default String map(final ProductId productId) {
        return productId.toString();
    }

    default String map(final SellerId sellerId) {
        return sellerId.getValue();
    }

    default String map(final Sku sku) {
        return sku.toString();
    }

    default String map(final Optional<String> value) {
        return value.orElse(null);
    }
}
