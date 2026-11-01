package com.shopflow.catalog.infrastructure.web.mapper;

import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.api.command.ChangeVariantPriceCommand;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.api.command.PublishProductCommand;
import com.shopflow.catalog.application.api.command.UpdateProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.ChangeVariantPriceRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.MoneyRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.UpdateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import com.shopflow.kernel.money.Money;
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

    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "productId", source = "productId")
    AddVariantCommand toCommand(AddVariantRequestDto requestDto, SellerId sellerId, ProductId productId);

    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "productId", source = "productId")
    UpdateProductCommand toCommand(UpdateProductRequestDto requestDto, SellerId sellerId, ProductId productId);

    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "sku", source = "sku")
    ChangeVariantPriceCommand toCommand(ChangeVariantPriceRequestDto requestDto, SellerId sellerId, ProductId productId,
                                        Sku sku);

    @Mapping(target = "sellerId", source = "sellerId")
    @Mapping(target = "productId", source = "productId")
    PublishProductCommand toPublishCommand(SellerId sellerId, ProductId productId);

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

    default Money map(final MoneyRequestDto price) {
        return Money.of(price.getAmount(), price.getCurrency());
    }

    default String map(final Optional<String> value) {
        return value.orElse(null);
    }
}
