package com.shopflow.catalog.infrastructure.kafka.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductVariant;
import com.shopflow.contracts.product.ProductVariantAdded;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversions du domaine vers les événements Avro. Le code est généré par MapStruct ({@code ProductEventMapperImpl}).
 */
@Mapper
public interface ProductEventMapper {

    /**
     * Un produit naît sans variante : celles ajoutées ensuite seront annoncées par {@code ProductVariantAdded}.
     */
    @Mapping(target = "productId", source = "id")
    @Mapping(target = "variants", expression = "java(java.util.List.of())")
    ProductCreated toProductCreated(Product product);

    @Mapping(target = "variantBuilder", ignore = true)
    ProductVariantAdded toProductVariantAdded(ProductId productId, Variant variant);

    @Mapping(target = "priceBuilder", ignore = true)
    ProductVariant toProductVariant(Variant variant);

    default String map(final ProductId productId) {
        return productId.toString();
    }

    default String map(final SellerId sellerId) {
        return sellerId.getValue();
    }

    default String map(final Sku sku) {
        return sku.toString();
    }
}
