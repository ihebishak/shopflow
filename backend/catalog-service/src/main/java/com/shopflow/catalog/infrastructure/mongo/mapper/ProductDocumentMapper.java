package com.shopflow.catalog.infrastructure.mongo.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.document.VariantDocument;
import com.shopflow.kernel.money.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Conversions entre le domaine et les documents MongoDB.
 * <p>
 * Produit → document : généré par MapStruct ({@code ProductDocumentMapperImpl}).
 * Document → produit : écrit à la main, car le domaine cache son constructeur et ne se reconstruit
 * que par {@link Product#restore}, que MapStruct ne sait pas appeler seul.
 */
@Mapper
public interface ProductDocumentMapper {

    ProductDocument toDocument(Product product);

    @Mapping(target = "priceAmount", source = "price.amount")
    @Mapping(target = "priceCurrency", source = "price.currency")
    VariantDocument toDocument(Variant variant);

    default Product toDomain(final ProductDocument document) {
        List<Variant> variants = document.getVariants() == null
                ? List.of()
                : document.getVariants().stream().map(this::toDomain).toList();
        return Product.restore(new ProductId(UUID.fromString(document.getId())), new SellerId(document.getSellerId()),
                document.getName(), document.getBrand(), document.getCategory(), document.getDescription(),
                document.getStatus(), document.getCreatedAt(), variants, document.getVersion());
    }

    default Variant toDomain(final VariantDocument document) {
        return Variant.restore(new Sku(UUID.fromString(document.getSku())), document.getSize(), document.getColor(),
                Money.of(document.getPriceAmount(), document.getPriceCurrency()));
    }

    default String map(final ProductId productId) {
        return productId.toString();
    }

    default String map(final SellerId sellerId) {
        return sellerId.getValue();
    }

    default String map(final Sku sku) {
        return sku.toString();
    }

    default String map(final Currency currency) {
        return currency.getCurrencyCode();
    }

    default String map(final Optional<String> value) {
        return value.orElse(null);
    }
}
