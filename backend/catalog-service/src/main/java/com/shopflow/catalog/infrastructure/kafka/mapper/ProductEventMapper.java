package com.shopflow.catalog.infrastructure.kafka.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductPriceChanged;
import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.contracts.product.ProductUpdated;
import com.shopflow.contracts.product.ProductVariant;
import com.shopflow.contracts.product.ProductVariantAdded;
import com.shopflow.kernel.money.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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

    ProductUpdated toProductUpdated(ProductId productId, List<String> changedFields);

    @Mapping(target = "oldPriceBuilder", ignore = true)
    @Mapping(target = "newPriceBuilder", ignore = true)
    ProductPriceChanged toProductPriceChanged(ProductId productId, Sku sku, Money oldPrice, Money newPrice);

    @Mapping(target = "productId", source = "id")
    ProductPublished toProductPublished(Product product);

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
